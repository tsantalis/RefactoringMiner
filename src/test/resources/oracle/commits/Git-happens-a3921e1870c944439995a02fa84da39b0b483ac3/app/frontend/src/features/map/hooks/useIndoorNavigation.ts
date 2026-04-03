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

  const handleSearch = (text: string, setPoint: React.Dispatch<React.SetStateAction<string>>, setSelectedNode: React.Dispatch<React.SetStateAction<LocalizedNode | null>>, setResults: React.Dispatch<React.SetStateAction<LocalizedNode[]>>) => {
    setPoint(text);
    setSelectedNode(null);
    setResults(filterNodes(text));
  };

  const handleStartSearch = (text: string) => {
    handleSearch(text, setStartPoint, setSelectedStartNode, setStartResults);
  };

  const handleDestinationSearch = (text: string) => {
    handleSearch(text, setDestinationPoint, setSelectedDestinationNode, setDestinationResults);
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