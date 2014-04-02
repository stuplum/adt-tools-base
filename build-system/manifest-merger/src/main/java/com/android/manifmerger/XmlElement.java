/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.manifmerger;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.utils.ILogger;
import com.android.utils.PositionXmlParser;
import com.android.utils.PositionXmlParser.Position;
import com.android.utils.SdkUtils;
import com.android.utils.XmlUtils;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Xml {@link org.w3c.dom.Element} which is mergeable.
 *
 * A mergeable element can contains 3 types of children :
 * <ul>
 *     <li>a child element, which itself may or may not be mergeable.</li>
 *     <li>xml attributes which are related to the element.</li>
 *     <li>tools oriented attributes to trigger specific behaviors from the merging tool</li>
 * </ul>
 *
 * The two main responsibilities of this class is to be capable of comparing itself against
 * another instance of the same type as well as providing XML element merging capabilities.
 */
public class XmlElement extends XmlNode {

    @NonNull private final Element mXml;
    @NonNull private final ManifestModel.NodeTypes mType;
    @NonNull private final XmlDocument mDocument;

    private final NodeOperationType mNodeOperationType;
    // list of non tools related attributes.
    private final ImmutableList<XmlAttribute> mAttributes;
    // map of all tools related attributes keyed by target attribute name
    private final Map<NodeName, AttributeOperationType> mAttributesOperationTypes;
    // list of mergeable children elements.
    private final ImmutableList<XmlElement> mMergeableChildren;
    // optional selector declared on this xml element.
    @Nullable private final Selector mSelector;

    public XmlElement(@NonNull Element xml, @NonNull XmlDocument document) {

        mXml = Preconditions.checkNotNull(xml);
        mType = ManifestModel.NodeTypes.fromXmlSimpleName(mXml.getNodeName());
        mDocument = Preconditions.checkNotNull(document);
        Selector selector = null;

        ImmutableMap.Builder<NodeName, AttributeOperationType> attributeOperationTypeBuilder =
                ImmutableMap.builder();
        ImmutableList.Builder<XmlAttribute> attributesListBuilder = ImmutableList.builder();
        NamedNodeMap namedNodeMap = mXml.getAttributes();
        NodeOperationType lastNodeOperationType = null;
        for (int i = 0; i < namedNodeMap.getLength(); i++) {
            Node attribute = namedNodeMap.item(i);
            if (SdkConstants.TOOLS_URI.equals(attribute.getNamespaceURI())) {
                String instruction = attribute.getLocalName();
                if (instruction.equals(NodeOperationType.NODE_LOCAL_NAME)) {
                    // should we flag an error when there are more than one operation type on a node ?
                    lastNodeOperationType = NodeOperationType.valueOf(
                            SdkUtils.camelCaseToConstantName(
                                    attribute.getNodeValue()));
                } else if (instruction.equals(Selector.SELECTOR_LOCAL_NAME)) {
                    selector = new Selector(attribute.getNodeValue());
                } else {
                    AttributeOperationType attributeOperationType =
                            AttributeOperationType.valueOf(
                                    SdkUtils.xmlNameToConstantName(instruction));
                    for (String attributeName : Splitter.on(',').trimResults()
                            .split(attribute.getNodeValue())) {
                        if (attributeName.indexOf(XmlUtils.NS_SEPARATOR) == -1) {
                            String toolsPrefix = XmlUtils
                                    .lookupNamespacePrefix(getXml(), SdkConstants.TOOLS_URI,
                                            SdkConstants.ANDROID_NS_NAME, false);
                            // automatically provide the prefix.
                            attributeName = toolsPrefix + XmlUtils.NS_SEPARATOR + attributeName;
                        }
                        NodeName nodeName = XmlNode.fromXmlName(attributeName);
                        attributeOperationTypeBuilder.put(nodeName, attributeOperationType);
                    }
                }
            }
        }
        mAttributesOperationTypes = attributeOperationTypeBuilder.build();
        for (int i = 0; i < namedNodeMap.getLength(); i++) {
            Node attribute = namedNodeMap.item(i);
            if (!SdkConstants.TOOLS_URI.equals(attribute.getNamespaceURI())) {

                XmlAttribute xmlAttribute = new XmlAttribute(
                        this, (Attr) attribute, mType.getAttributeModel(XmlNode.fromXmlName(
                                ((Attr) attribute).getName())));
                attributesListBuilder.add(xmlAttribute);
            }

        }
        mNodeOperationType = lastNodeOperationType;
        mAttributes = attributesListBuilder.build();
        mMergeableChildren = initMergeableChildren();
        mSelector = selector;
    }

    /**
     * Returns true if this xml element's {@link com.android.manifmerger.ManifestModel.NodeTypes} is
     * the passed one.
     */
    public boolean isA(ManifestModel.NodeTypes type) {
        return this.mType == type;
    }

    @NonNull
    @Override
    public Element getXml() {
        return mXml;
    }


    @Override
    public String getId() {
        return Strings.isNullOrEmpty(getKey())
                ? getName().toString()
                : getName().toString() + "#" + getKey();
    }

    @Override
    public NodeName getName() {
        return XmlNode.unwrapName(mXml);
    }

    /**
     * Returns the owning {@link com.android.manifmerger.XmlDocument}
     */
    @NonNull
    public XmlDocument getDocument() {
        return mDocument;
    }

    /**
     * Returns this xml element {@link com.android.manifmerger.ManifestModel.NodeTypes}
     */
    @NonNull
    public ManifestModel.NodeTypes getType() {
        return mType;
    }

    /**
     * Returns the unique key for this xml element within the xml file or null if there can be only
     * one element of this type.
     */
    @Nullable
    public String getKey() {
        return mType.getNodeKeyResolver().getKey(this);
    }

    /**
     * Returns the list of attributes for this xml element.
     */
    public List<XmlAttribute> getAttributes() {
        return mAttributes;
    }

    /**
     * Returns the {@link com.android.manifmerger.XmlAttribute} for an attribute present on this
     * xml element, or {@link com.google.common.base.Optional#absent} if not present.
     * @param attributeName the attribute name.
     */
    public Optional<XmlAttribute> getAttribute(NodeName attributeName) {
        for (XmlAttribute xmlAttribute : mAttributes) {
            if (xmlAttribute.getName().equals(attributeName)) {
                return Optional.of(xmlAttribute);
            }
        }
        return Optional.absent();
    }

    /**
     * Get the node operation type as optionally specified by the user. If the user did not
     * explicitly specify how conflicting elements should be handled, a
     * {@link com.android.manifmerger.NodeOperationType#MERGE} will be returned.
     */
    public NodeOperationType getOperationType() {
        return mNodeOperationType != null
                ? mNodeOperationType
                : NodeOperationType.MERGE;
    }

    /**
     * Get the attribute operation type as optionally specified by the user. If the user did not
     * explicitly specify how conflicting attributes should be handled, a
     * {@link AttributeOperationType#STRICT} will be returned.
     */
    public AttributeOperationType getAttributeOperationType(NodeName attributeName) {
        return mAttributesOperationTypes.containsKey(attributeName)
                ? mAttributesOperationTypes.get(attributeName)
                : AttributeOperationType.STRICT;
    }

    public Collection<Map.Entry<NodeName, AttributeOperationType>> getAttributeOperations() {
        return mAttributesOperationTypes.entrySet();
    }


    @Override
    public PositionXmlParser.Position getPosition() {
        return mDocument.getNodePosition(this);
    }

    public void printPosition(StringBuilder stringBuilder) {
        PositionXmlParser.Position position = getPosition();
        if (position == null) {
            stringBuilder.append("Unknown position");
            return;
        }
        dumpPosition(stringBuilder, position);
    }

    public String printPosition() {
        StringBuilder stringBuilder = new StringBuilder();
        printPosition(stringBuilder);
        return stringBuilder.toString();
    }

    private void dumpPosition(StringBuilder stringBuilder, Position position) {
      stringBuilder
          .append("(").append(position.getLine())
          .append(",").append(position.getColumn()).append(") ")
          .append(mDocument.getSourceLocation().print(true))
          .append(":").append(position.getLine());
    }

    /**
     * Merge this xml element with a lower priority node.
     *
     * This is WIP.
     *
     * For now, attributes will be merged. If present on both xml elements, a warning will be
     * issued and the attribute merge will be rejected.
     *
     * @param lowerPriorityNode lower priority Xml element to merge with.
     * @param mergingReport the merging report to log errors and actions.
     */
    public void mergeWithLowerPriorityNode(
            XmlElement lowerPriorityNode,
            MergingReport.Builder mergingReport) {

        mergingReport.getLogger().info("Merging " + getId()
                + " with lower " + lowerPriorityNode.printPosition());

        if (getType().getMergeType() != MergeType.MERGE_CHILDREN_ONLY) {
            // make a copy of all the attributes metadata, it will eliminate elements from this
            // list as it finds them explicitly defined in the lower priority node.
            // At the end of the explicit attributes processing, the remaining elements of this
            // list will need to be checked for default value that may clash with a locally
            // defined attribute.
            List<AttributeModel> attributeModels =
                    new ArrayList<AttributeModel>(lowerPriorityNode.getType().getAttributeModels());

            // merge explicit attributes from lower priority node.
            for (XmlAttribute lowerPriorityAttribute : lowerPriorityNode.getAttributes()) {
                lowerPriorityAttribute.mergeInHigherPriorityElement(this, mergingReport);
                if (lowerPriorityAttribute.getModel() != null) {
                    attributeModels.remove(lowerPriorityAttribute.getModel());
                }
            }
            // merge implicit default values from lower priority node when we have an explicit
            // attribute declared on this node.
            for (AttributeModel attributeModel : attributeModels) {
                if (attributeModel.getDefaultValue() != null) {
                    Optional<XmlAttribute> myAttribute = getAttribute(attributeModel.getName());
                    if (myAttribute.isPresent()) {
                        myAttribute.get().mergeWithLowerPriorityDefaultValue(
                                mergingReport, lowerPriorityNode);
                    }
                }
            }
        }
        // are we supposed to merge children ?
        if (mNodeOperationType != NodeOperationType.MERGE_ONLY_ATTRIBUTES) {
            mergeChildren(lowerPriorityNode, mergingReport);
        } else {
            // record rejection of the lower priority node's children .
            for (XmlElement lowerPriorityChild : lowerPriorityNode.getMergeableElements()) {
                mergingReport.getActionRecorder().recordNodeAction(this,
                        ActionRecorder.ActionType.REJECTED,
                        lowerPriorityChild);
            }
        }
    }

    public ImmutableList<XmlElement> getMergeableElements() {
        return mMergeableChildren;
    }

    public Optional<XmlElement> getNodeByTypeAndKey(
            ManifestModel.NodeTypes type,
            @Nullable String keyValue) {

        for (XmlElement xmlElement : mMergeableChildren) {
            if (xmlElement.isA(type) &&
                    (keyValue == null || keyValue.equals(xmlElement.getKey()))) {
                return Optional.of(xmlElement);
            }
        }
        return Optional.absent();
    }

    // merge this higher priority node with a lower priority node.
    public void mergeChildren(XmlElement lowerPriorityNode,
            MergingReport.Builder mergingReport) {

        ILogger logger = mergingReport.getLogger();
        // read all lower priority mergeable nodes.
        // if the same node is not defined in this document merge it in.
        // if the same is defined, so far, give an error message.
        for (XmlElement lowerPriorityChild : lowerPriorityNode.getMergeableElements()) {

            if (shouldIgnore(lowerPriorityChild, mergingReport)) {
                continue;
            }

            Optional<XmlElement> thisChildOptional =
                    getNodeByTypeAndKey(lowerPriorityChild.getType(),lowerPriorityChild.getKey());

            // only in the lower priority document ?
            if (!thisChildOptional.isPresent()) {
                addElement(lowerPriorityChild, mergingReport);
                continue;
            }
            // it's defined in both files.
            logger.verbose(lowerPriorityChild.getId() + " defined in both files...");

            XmlElement thisChild = thisChildOptional.get();
            switch (thisChild.getType().getMergeType()) {
                case CONFLICT:
                    mergingReport.addError(String.format(
                            "Node %1$s cannot be present in more than one input file and it's "
                                    + "present at %2$s and %3$s",
                            thisChild.getType(),
                            thisChild.printPosition(),
                            lowerPriorityChild.printPosition()));
                    break;
                case ALWAYS:
                    // no merging, we consume the lower priority node unmodified.
                    // if the two elements are equal, just skip it.
                    if (thisChild.compareTo(lowerPriorityChild).isPresent()) {
                        addElement(lowerPriorityChild, mergingReport);
                    }
                    break;
                default:
                    // 2 nodes exist, some merging need to happen
                    handleTwoElementsExistence(thisChild, lowerPriorityChild, mergingReport);
            }
        }
    }

    /**
     * Determine if we should completely ignore a child from any merging activity.
     * There are 2 situations where we should ignore a lower priority child :
     * <p>
     * <ul>
     *     <li>The associate {@link com.android.manifmerger.ManifestModel.NodeTypes} is
     *     annotated with {@link com.android.manifmerger.MergeType#IGNORE}</li>
     *     <li>This element has a child of the same type with no key that has a '
     *     tools:node="removeAll' attribute.</li>
     * </ul>
     * @param lowerPriorityChild the lower priority child we should determine eligibility for
     *                           merging.
     * @return true if the element should be ignored, false otherwise.
     */
    private boolean shouldIgnore(
            XmlElement lowerPriorityChild,
            MergingReport.Builder mergingReport) {

        if (lowerPriorityChild.getType().getMergeType() == MergeType.IGNORE) {
            return true;
        }

        // do we have an element of the same type of that child with no key ?
        Optional<XmlElement> thisChildElementOptional =
                getNodeByTypeAndKey(lowerPriorityChild.getType(), null /* keyValue */);
        if (!thisChildElementOptional.isPresent()) {
            return false;
        }
        XmlElement thisChild = thisChildElementOptional.get();

        // are we supposed to delete all occurrences and if yes, is there a selector defined to
        // filter which elements should be deleted.
        boolean shouldDelete = thisChild.mNodeOperationType == NodeOperationType.REMOVE_ALL
                && (thisChild.mSelector == null
                        || thisChild.mSelector.appliesTo(lowerPriorityChild));
        // if we should discard this child element, record the action.
        if (shouldDelete) {
            mergingReport.getActionRecorder().recordNodeAction(thisChildElementOptional.get(),
                    ActionRecorder.ActionType.REJECTED,
                    lowerPriorityChild);
        }
        return shouldDelete;
    }

    /**
     * Handle 2 elements (of same identity) merging.
     * higher priority one has a tools:node="remove", remove the low priority one
     * higher priority one has a tools:node="replace", replace the low priority one
     * higher priority one has a tools:node="strict", flag the error if not equals.
     * default or tools:node="merge", merge the two elements.
     * @param higherPriority the higher priority node.
     * @param lowerPriority the lower priority element.
     * @param mergingReport the merging report to log errors and actions.
     */
    private static void handleTwoElementsExistence(
            XmlElement higherPriority,
            XmlElement lowerPriority,
            MergingReport.Builder mergingReport) {

        NodeOperationType operationType = calculateNodeOperationType(higherPriority, lowerPriority);
        // 2 nodes exist, 3 possibilities :
        //  higher priority one has a tools:node="remove", remove the low priority one
        //  higher priority one has a tools:node="replace", replace the low priority one
        //  higher priority one has a tools:node="strict", flag the error if not equals.
        switch(operationType) {
            case MERGE:
            case MERGE_ONLY_ATTRIBUTES:
                // record the action
                mergingReport.getActionRecorder().recordNodeAction(higherPriority,
                        ActionRecorder.ActionType.MERGED, lowerPriority);
                // and perform the merge
                higherPriority.mergeWithLowerPriorityNode(lowerPriority, mergingReport);
                break;
            case REMOVE:
            case REPLACE:
                // so far remove and replace and similar, the post validation will take
                // care of removing this node in the case of REMOVE.

                // just don't import the lower priority node and record the action.
                mergingReport.getActionRecorder().recordNodeAction(higherPriority,
                        ActionRecorder.ActionType.REJECTED, lowerPriority);
                break;
            case STRICT:
                Optional<String> compareMessage = higherPriority.compareTo(lowerPriority);
                if (compareMessage.isPresent()) {
                    // flag error.
                    mergingReport.addError(String.format(
                            "Node %1$s at %2$s is tagged with tools:node=\"strict\", yet "
                                    + "%3$s at %4$s is different : %5$s",
                            higherPriority.getId(),
                            higherPriority.printPosition(),
                            lowerPriority.getId(),
                            lowerPriority.printPosition(),
                            compareMessage.get()
                    ));
                }
                break;
            default:
                mergingReport.getLogger().error(null /* throwable */,
                        "Unhandled node operation type %s", higherPriority.getOperationType());
                break;
        }
    }

    /**
     * Calculate the effective node operation type for a higher priority node when a lower priority
     * node is queried for merge.
     * @param higherPriority the higher priority node which may have a {@link NodeOperationType}
     *                       declaration and may also have a {@link Selector} declaration.
     * @param lowerPriority the lower priority node that is elected for merging with the higher
     *                      priority node.
     * @return the effective {@link NodeOperationType} that should be used to affect higher and
     * lower priority nodes merging.
     */
    private static NodeOperationType calculateNodeOperationType(
            @NonNull XmlElement higherPriority,
            @NonNull XmlElement lowerPriority) {

        NodeOperationType operationType = higherPriority.getOperationType();
        // if the operation's selector exists and the lower priority node is not selected,
        // we revert to default operation type which is merge.
        if (operationType.isSelectable()
                && higherPriority.mSelector != null
                && !higherPriority.mSelector.appliesTo(lowerPriority)) {
            operationType = NodeOperationType.MERGE;
        }
        return operationType;
    }

    /**
     * Add an element and its leading comments as the last sub-element of the current element.
     * @param elementToBeAdded xml element to be added to the current element.
     * @param mergingReport the merging report to log errors and actions.
     */
    private void addElement(XmlElement elementToBeAdded, MergingReport.Builder mergingReport) {

        List<Node> comments = getLeadingComments(elementToBeAdded.getXml());
        // only in the new file, just import it.
        Node node = mXml.getOwnerDocument().adoptNode(elementToBeAdded.getXml());
        mXml.appendChild(node);

        // also adopt the child's comments if any.
        for (Node comment : comments) {
            Node newComment = mXml.getOwnerDocument().adoptNode(comment);
            mXml.insertBefore(newComment, node);
        }

        mergingReport.getActionRecorder().recordNodeAction(elementToBeAdded,
                ActionRecorder.ActionType.ADDED);
        mergingReport.getLogger().verbose("Adopted " + node);
    }

    /**
     * Compares this element with another {@link XmlElement} ignoring all attributes belonging to
     * the {@link com.android.SdkConstants#TOOLS_URI} namespace.
     *
     * @param otherNode the other element to compare against.
     * @return a {@link String} describing the differences between the two XML elements or
     * {@link Optional#absent()} if they are equals.
     */
    public Optional<String> compareTo(XmlElement otherNode) {

        // compare element names
        if (mXml.getNamespaceURI() != null) {
            if (!mXml.getLocalName().equals(otherNode.mXml.getLocalName())) {
                return Optional.of(
                        String.format("Element names do not match: %1$s versus %2$s",
                                mXml.getLocalName(),
                                otherNode.mXml.getLocalName()));
            }
            // compare element ns
            String thisNS = mXml.getNamespaceURI();
            String otherNS = otherNode.mXml.getNamespaceURI();
            if ((thisNS == null && otherNS != null)
                    || (thisNS != null && !thisNS.equals(otherNS))) {
                return Optional.of(
                        String.format("Element namespaces names do not match: %1$s versus %2$s",
                                thisNS, otherNS));
            }
        } else {
            if (!mXml.getNodeName().equals(otherNode.mXml.getNodeName())) {
                return Optional.of(String.format("Element names do not match: %1$s versus %2$s",
                        mXml.getNodeName(),
                        otherNode.mXml.getNodeName()));
            }
        }

        // compare attributes, we do it twice to identify added/missing elements in both lists.
        Optional<String> message = checkAttributes(this, otherNode);
        if (message.isPresent()) {
            return message;
        }
        message = checkAttributes(otherNode, this);
        if (message.isPresent()) {
            return message;
        }

        // compare children
        List<Node> expectedChildren = filterUninterestingNodes(mXml.getChildNodes());
        List<Node> actualChildren = filterUninterestingNodes(otherNode.mXml.getChildNodes());
        if (expectedChildren.size() != actualChildren.size()) {
            return Optional.of(String.format(
                    "%1$s: Number of children do not match up: expected %2$d versus %3$d at %4$s",
                    getId(),
                    expectedChildren.size(),
                    actualChildren.size(),
                    otherNode.printPosition()));
        }
        for (Node expectedChild : expectedChildren) {
            if (expectedChild.getNodeType() == Node.ELEMENT_NODE) {
                XmlElement expectedChildNode = new XmlElement((Element) expectedChild, mDocument);
                message = findAndCompareNode(otherNode, actualChildren, expectedChildNode);
                if (message.isPresent()) {
                    return message;
                }
            }
        }
        return Optional.absent();
    }

    private Optional<String> findAndCompareNode(
            XmlElement otherElement,
            List<Node> otherElementChildren,
            XmlElement childNode) {

        for (Node potentialNode : otherElementChildren) {
            if (potentialNode.getNodeType() == Node.ELEMENT_NODE) {
                XmlElement otherChildNode = new XmlElement((Element) potentialNode, mDocument);
                if (childNode.getType() == otherChildNode.getType()
                        && ((childNode.getKey() == null && otherChildNode.getKey() == null)
                        || childNode.getKey().equals(otherChildNode.getKey()))) {
                    return childNode.compareTo(otherChildNode);
                }
            }
        }
        return Optional.of(String.format("Child %1$s not found in document %2$s",
                childNode.getId(),
                otherElement.printPosition()));
    }

    private static List<Node> filterUninterestingNodes(NodeList nodeList) {
        List<Node> interestingNodes = new ArrayList<Node>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.TEXT_NODE) {
                Text t = (Text) node;
                if (!t.getData().trim().isEmpty()) {
                    interestingNodes.add(node);
                }
            } else if (node.getNodeType() != Node.COMMENT_NODE) {
                interestingNodes.add(node);
            }

        }
        return interestingNodes;
    }

    private static Optional<String> checkAttributes(
            XmlElement expected,
            XmlElement actual) {

        for (XmlAttribute expectedAttr : expected.getAttributes()) {
            XmlAttribute.NodeName attributeName = expectedAttr.getName();
            if (attributeName.isInNamespace(SdkConstants.TOOLS_URI)) {
                continue;
            }
            Optional<XmlAttribute> actualAttr = actual.getAttribute(attributeName);
            if (actualAttr.isPresent()) {
                if (!expectedAttr.getValue().equals(actualAttr.get().getValue())) {
                    return Optional.of(
                            String.format("Attribute %1$s do not match: %2$s versus %3$s at %4$s",
                                    expectedAttr.getId(),
                                    expectedAttr.getValue(),
                                    actualAttr.get().getValue(),
                                    actual.printPosition()));
                }
            } else {
                return Optional.of(String.format("Attribute %1$s not found at %2$s",
                        expectedAttr.getId(), actual.printPosition()));
            }
        }
        return Optional.absent();
    }

    private ImmutableList<XmlElement> initMergeableChildren() {
        ImmutableList.Builder<XmlElement> mergeableNodes = new ImmutableList.Builder<XmlElement>();
        NodeList nodeList = mXml.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node instanceof Element) {
                XmlElement xmlElement = new XmlElement((Element) node, mDocument);
                mergeableNodes.add(xmlElement);
            }
        }
        return mergeableNodes.build();
    }

    /**
     * Returns all leading comments in the source xml before the node to be adopted.
     * @param nodeToBeAdopted node that will be added as a child to this node.
     */
    private static List<Node> getLeadingComments(Node nodeToBeAdopted) {
        ImmutableList.Builder<Node> nodesToAdopt = new ImmutableList.Builder<Node>();
        Node previousSibling = nodeToBeAdopted.getPreviousSibling();
        while (previousSibling != null
                && (previousSibling.getNodeType() == Node.COMMENT_NODE
                || previousSibling.getNodeType() == Node.TEXT_NODE)) {
            // we really only care about comments.
            if (previousSibling.getNodeType() == Node.COMMENT_NODE) {
                nodesToAdopt.add(previousSibling);
            }
            previousSibling = previousSibling.getPreviousSibling();
        }
        return nodesToAdopt.build().reverse();
    }
}