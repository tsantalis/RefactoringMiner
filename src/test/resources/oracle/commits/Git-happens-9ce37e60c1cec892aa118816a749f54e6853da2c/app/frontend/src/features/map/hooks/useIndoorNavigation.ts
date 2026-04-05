import { useState } from "react";
import { LocalizedNode } from "../../../services/floorPlanService";

export const useIndoorNavigation = (nodes: LocalizedNode[]) => {
  const [startPoint, setStartPoint] = useState("");
  const [destinationPoint, setDestinationPoint] = useState("");

  const [startResults, setStartResults] = useState<LocalizedNode[]>([]);
  const [destinationResults, setDestinationResults] = useState<LocalizedNode[]>([]);

  const [selectedStartNode, setSelectedStartNode] = useState<LocalizedNode | null>(null);
  const [selectedDestinationNode, setSelectedDestinationNode] = useState<LocalizedNode | null>(null);

  const filterNodes = (text: string) => {
    if (!text) return [];

    return nodes.filter((node) =>
      (node.label || "").toLowerCase().includes(text.toLowerCase())
    );
  };

  const handleStartSearch = (text: string) => {
    setStartPoint(text);
    setSelectedStartNode(null);
    setStartResults(filterNodes(text));
  };

  const handleDestinationSearch = (text: string) => {
    setDestinationPoint(text);
    setSelectedDestinationNode(null);
    setDestinationResults(filterNodes(text));
  };

  const selectStartNode = (node: LocalizedNode) => {
    setSelectedStartNode(node);
    setStartPoint(node.label);
    setStartResults([]);
  };

  const selectDestinationNode = (node: LocalizedNode) => {
    setSelectedDestinationNode(node);
    setDestinationPoint(node.label);
    setDestinationResults([]);
  };

  const handleStartNavigation = () => {
    if (!selectedStartNode || !selectedDestinationNode) {
      console.warn("Select both points first");
      return;
    }

    console.log(
    `[NAV] ${selectedStartNode.label} → ${selectedDestinationNode.label}`
    );

    // to be implemented later: Trigger pathfinding algorithm
  };

  const swapPoints = () => {
    setStartPoint(destinationPoint);
    setDestinationPoint(startPoint);
    setSelectedStartNode(selectedDestinationNode);
    setSelectedDestinationNode(selectedStartNode);
    setStartResults([]);
    setDestinationResults([]);
  };

  return {
    startPoint,
    destinationPoint,
    startResults,
    destinationResults,
    handleStartSearch,
    handleDestinationSearch,
    selectStartNode,
    selectDestinationNode,
    handleStartNavigation,
    swapPoints,
  };
};