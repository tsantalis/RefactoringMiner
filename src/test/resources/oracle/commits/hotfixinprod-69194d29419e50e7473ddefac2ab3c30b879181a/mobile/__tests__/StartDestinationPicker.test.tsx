import React from 'react';
import { render, waitFor, act, fireEvent } from '@testing-library/react-native';
import StartDestinationPicker from '../src/components/BuildingSelector/StartDestinationPicker';
import BuildingSelector from '../src/components/BuildingSelector/BuildingSelector';
import * as Location from 'expo-location';

// Mock expo-location
jest.mock('expo-location', () => ({
  getCurrentPositionAsync: jest.fn(),
  reverseGeocodeAsync: jest.fn(),
}));

// Mock BuildingSelector component
jest.mock('../src/components/BuildingSelector/BuildingSelector', () => {
  const React = require('react');
  const { View } = require('react-native');
  return jest.fn((props) => {
    return React.createElement(View, { 
      testID: `building-selector-${props.placeholder}`,
      onPress: () => props.onSelect({
        name: 'Mock Building',
        address: '123 Mock St',
        location: { lat: 1, lng: 1 }
      })
    });
  });
});

// Mock buildings data
jest.mock('../src/data/buildings', () => ({
  buildings: [
    {
      id: 'Hall Building',
      address: '1455 De Maisonneuve Blvd. W.',
      labelCoord: { latitude: 45.497, longitude: -73.579 }
    },
    {
      id: 'EV Building',
      address: '1515 St. Catherine St. W.',
      labelCoord: { latitude: 45.495, longitude: -73.578 }
    }
  ]
}));

describe('StartDestinationPicker', () => {
  // Helper functions to reduce code duplication
  const getStartSelector = () => {
    const calls = (BuildingSelector as jest.Mock).mock.calls;
    const startCall = calls.find(call => call[0].placeholder === 'Select start building');
    return startCall[0].onSelect;
  };

  const getDestinationSelector = () => {
    const calls = (BuildingSelector as jest.Mock).mock.calls;
    const destCall = calls.find(call => call[0].placeholder === 'Select destination building');
    return destCall[0].onSelect;
  };

  const createMockPlace = (name: string, address: string, lat: number = 40.7128, lng: number = -74.006) => ({
    name,
    address,
    location: { lat, lng },
  });

  const selectPlace = async (onSelect: (place: any) => void, place: any) => {
    await act(async () => {
      onSelect(place);
    });
  };

  const setupLocationMock = (coords: { latitude: number; longitude: number }) => {
    (Location.getCurrentPositionAsync as jest.Mock).mockResolvedValue({ coords });
  };

  const pressCurrentLocationButton = async (getByText: any) => {
    const currentLocationButton = getByText('Use Current Location');
    await act(async () => {
      fireEvent.press(currentLocationButton);
    });
  };

  beforeEach(() => {
    jest.clearAllMocks();
    console.log = jest.fn();
    (Location.getCurrentPositionAsync as jest.Mock).mockClear();
    (Location.reverseGeocodeAsync as jest.Mock).mockClear();
  });

  it('renders without crashing', () => {
    const { getByText } = render(<StartDestinationPicker userLocation={null} />);
    expect(getByText('Start Building')).toBeTruthy();
    expect(getByText('Destination Building')).toBeTruthy();
  });

  it('renders both BuildingSelector components', () => {
    render(<StartDestinationPicker userLocation={null} />);
    expect(BuildingSelector).toHaveBeenCalledTimes(2);
  });

  it('passes correct placeholder for start building selector', () => {
    render(<StartDestinationPicker userLocation={null} />);
    const calls = (BuildingSelector as jest.Mock).mock.calls;
    const startCall = calls.find(call => call[0].placeholder === 'Select start building');
    expect(startCall).toBeDefined();
    expect(startCall[0].placeholder).toBe('Select start building');
  });

  it('passes correct placeholder for destination building selector', () => {
    render(<StartDestinationPicker userLocation={null} />);
    const calls = (BuildingSelector as jest.Mock).mock.calls;
    const destCall = calls.find(call => call[0].placeholder === 'Select destination building');
    expect(destCall).toBeDefined();
    expect(destCall[0].placeholder).toBe('Select destination building');
  });

  it('handles start building selection', async () => {
    const { getByText } = render(<StartDestinationPicker userLocation={null} />);
    const onSelectStart = getStartSelector();
    const mockPlace = createMockPlace('Start Building', '123 Start St');

    await selectPlace(onSelectStart, mockPlace);

    await waitFor(() => {
      expect(getByText('Selected: Start Building')).toBeTruthy();
    });
  });

  it('handles destination building selection', async () => {
    const { getByText } = render(<StartDestinationPicker userLocation={null} />);
    const onSelectDest = getDestinationSelector();
    const mockPlace = createMockPlace('Destination Building', '456 Dest Ave', 41.8781, -87.6298);

    await selectPlace(onSelectDest, mockPlace);

    await waitFor(() => {
      expect(getByText('Selected: Destination Building')).toBeTruthy();
    });
  });

  it('logs start building selection in useEffect', async () => {
    render(<StartDestinationPicker userLocation={null} />);
    const onSelectStart = getStartSelector();
    const mockPlace = createMockPlace('Start Building', '123 Start St');

    await selectPlace(onSelectStart, mockPlace);

    await waitFor(() => {
      expect(console.log).toHaveBeenCalledWith('Start building selected:', mockPlace);
    });
  });

  it('logs destination building selection in useEffect', async () => {
    render(<StartDestinationPicker userLocation={null} />);
    const onSelectDest = getDestinationSelector();
    const mockPlace = createMockPlace('Destination Building', '456 Dest Ave', 41.8781, -87.6298);

    await selectPlace(onSelectDest, mockPlace);

    await waitFor(() => {
      expect(console.log).toHaveBeenCalledWith('Destination building selected:', mockPlace);
    });
  });

  it('does not show selected text when no building is selected', () => {
    const { queryByText } = render(<StartDestinationPicker userLocation={null} />);
    expect(queryByText(/^Selected:/)).toBeNull();
  });

  it('handles multiple selections for start building', async () => {
    const { getByText, queryByText } = render(<StartDestinationPicker userLocation={null} />);
    const onSelectStart = getStartSelector();

    const mockPlace1 = createMockPlace('First Building', '123 First St');
    await selectPlace(onSelectStart, mockPlace1);

    await waitFor(() => {
      expect(getByText('Selected: First Building')).toBeTruthy();
    });

    const mockPlace2 = createMockPlace('Second Building', '456 Second St', 41.8781, -87.6298);
    await selectPlace(onSelectStart, mockPlace2);

    await waitFor(() => {
      expect(getByText('Selected: Second Building')).toBeTruthy();
      expect(queryByText('Selected: First Building')).toBeNull();
    });
  });

  it('handles independent selections for start and destination', async () => {
    const { getByText } = render(<StartDestinationPicker userLocation={null} />);
    const onSelectStart = getStartSelector();
    const onSelectDest = getDestinationSelector();

    const startPlace = createMockPlace('Start Building', '123 Start St');
    const destPlace = createMockPlace('Destination Building', '456 Dest Ave', 41.8781, -87.6298);

    await act(async () => {
      onSelectStart(startPlace);
      onSelectDest(destPlace);
    });

    await waitFor(() => {
      expect(getByText('Selected: Start Building')).toBeTruthy();
      expect(getByText('Selected: Destination Building')).toBeTruthy();
    });
  });

  it('applies correct styles to container', () => {
    const { getByText } = render(<StartDestinationPicker userLocation={null} />);
    const container = getByText('Start Building').parent?.parent;
    expect(container?.props.style).toBeDefined();
  });

  it('applies correct styles to labels', () => {
    const { getByText } = render(<StartDestinationPicker userLocation={null} />);
    const startLabel = getByText('Start Building');
    const destLabel = getByText('Destination Building');
    
    expect(startLabel.props.style).toBeDefined();
    expect(destLabel.props.style).toBeDefined();
  });

  it('shows Use Current Location button when userLocation is provided', () => {
    const userLocation = { latitude: 45.5, longitude: -73.6 };
    const { getByText } = render(<StartDestinationPicker userLocation={userLocation} />);
    expect(getByText('Use Current Location')).toBeTruthy();
  });

  it('does not show Use Current Location button when userLocation is null', () => {
    const { queryByText } = render(<StartDestinationPicker userLocation={null} />);
    expect(queryByText('Use Current Location')).toBeNull();
  });

  it('handles Use Current Location button press - finds nearest building', async () => {
    const userLocation = { latitude: 45.497, longitude: -73.579 };
    setupLocationMock(userLocation);

    const { getByText } = render(<StartDestinationPicker userLocation={userLocation} />);
    await pressCurrentLocationButton(getByText);

    await waitFor(() => {
      expect(getByText('Selected: Hall Building')).toBeTruthy();
    });
  });

  it('handles Use Current Location button press - uses reverse geocoding when far from buildings', async () => {
    const userLocation = { latitude: 40.7128, longitude: -74.006 };
    setupLocationMock(userLocation);
    (Location.reverseGeocodeAsync as jest.Mock).mockResolvedValue([{
      street: 'Broadway',
      city: 'New York',
      region: 'NY',
      name: 'Times Square'
    }]);

    const { getByText } = render(<StartDestinationPicker userLocation={userLocation} />);
    await pressCurrentLocationButton(getByText);

    await waitFor(() => {
      expect(getByText('Selected: Broadway')).toBeTruthy();
    });
  });

  it('handles Use Current Location button press - fallback to coordinates when reverse geocoding fails', async () => {
    const userLocation = { latitude: 40.7128, longitude: -74.006 };
    setupLocationMock(userLocation);
    (Location.reverseGeocodeAsync as jest.Mock).mockResolvedValue([]);

    const { getByText } = render(<StartDestinationPicker userLocation={userLocation} />);
    await pressCurrentLocationButton(getByText);

    await waitFor(() => {
      expect(getByText('Selected: Current Location')).toBeTruthy();
    });
  });

  it('handles Use Current Location button press - error handling with fallback', async () => {
    const userLocation = { latitude: 40.7128, longitude: -74.006 };
    (Location.getCurrentPositionAsync as jest.Mock).mockRejectedValue(new Error('Location error'));
    console.error = jest.fn();

    const { getByText } = render(<StartDestinationPicker userLocation={userLocation} />);
    await pressCurrentLocationButton(getByText);

    await waitFor(() => {
      expect(console.error).toHaveBeenCalledWith('Error getting location name:', expect.any(Error));
      expect(getByText('Selected: Current Location')).toBeTruthy();
    });
  });

  it('handles Use Current Location button press - error handling without fallback', async () => {
    (Location.getCurrentPositionAsync as jest.Mock).mockRejectedValue(new Error('Location error'));
    console.error = jest.fn();

    const { queryByText } = render(<StartDestinationPicker userLocation={null} />);
    // Wait for component to render - no Use Current Location button should appear
    expect(queryByText('Use Current Location')).toBeNull();
  });

  it('shows clear button when start building is selected', async () => {
    const { UNSAFE_getAllByType } = render(<StartDestinationPicker userLocation={null} />);
    const onSelectStart = getStartSelector();
    const mockPlace = createMockPlace('Start Building', '123 Start St');

    await selectPlace(onSelectStart, mockPlace);

    await waitFor(() => {
      const TouchableOpacity = require('react-native').TouchableOpacity;
      const clearButtons = UNSAFE_getAllByType(TouchableOpacity);
      expect(clearButtons.length).toBeGreaterThan(0);
    });
  });

  it('clears start building when clear button is pressed', async () => {
    const { getByText, queryByText, getByTestId } = render(<StartDestinationPicker userLocation={null} />);
    const onSelectStart = getStartSelector();
    const mockPlace = createMockPlace('Start Building', '123 Start St');

    await selectPlace(onSelectStart, mockPlace);

    await waitFor(() => {
      expect(getByText('Selected: Start Building')).toBeTruthy();
    });

    const clearButton = getByTestId('clear-start-button');
    
    await act(async () => {
      fireEvent.press(clearButton);
    });

    await waitFor(() => {
      expect(queryByText('Selected: Start Building')).toBeNull();
    });
  });

  it('shows clear button when destination building is selected', async () => {
    const { UNSAFE_getAllByType } = render(<StartDestinationPicker userLocation={null} />);
    const onSelectDest = getDestinationSelector();
    const mockPlace = createMockPlace('Destination Building', '456 Dest Ave', 41.8781, -87.6298);

    await selectPlace(onSelectDest, mockPlace);

    await waitFor(() => {
      const TouchableOpacity = require('react-native').TouchableOpacity;
      const clearButtons = UNSAFE_getAllByType(TouchableOpacity);
      expect(clearButtons.length).toBeGreaterThan(0);
    });
  });

  it('clears destination building when clear button is pressed', async () => {
    const { getByText, queryByText, getByTestId } = render(<StartDestinationPicker userLocation={null} />);
    const onSelectStart = getStartSelector();
    const onSelectDest = getDestinationSelector();

    const mockStartPlace = createMockPlace('Start Building', '123 Start St');
    const mockDestPlace = createMockPlace('Destination Building', '456 Dest Ave', 41.8781, -87.6298);

    await selectPlace(onSelectStart, mockStartPlace);
    await selectPlace(onSelectDest, mockDestPlace);

    await waitFor(() => {
      expect(getByText('Selected: Destination Building')).toBeTruthy();
      expect(getByText('Selected: Start Building')).toBeTruthy();
    });

    const destinationClearButton = getByTestId('clear-destination-button');

    await act(async () => {
      fireEvent.press(destinationClearButton);
    });

    await waitFor(() => {
      expect(queryByText('Selected: Destination Building')).toBeNull();
      expect(getByText('Selected: Start Building')).toBeTruthy();
    });
  });

  it('passes userLocation prop to BuildingSelector components', () => {
    const userLocation = { latitude: 45.5, longitude: -73.6 };
    render(<StartDestinationPicker userLocation={userLocation} />);
    
    const calls = (BuildingSelector as jest.Mock).mock.calls;
    calls.forEach(call => {
      expect(call[0].userLocation).toEqual(userLocation);
    });
  });

  it('shows loading indicator when fetching current location', async () => {
    const userLocation = { latitude: 45.5, longitude: -73.6 };
    let resolveLocation: any;
    const locationPromise = new Promise((resolve) => {
      resolveLocation = resolve;
    });
    (Location.getCurrentPositionAsync as jest.Mock).mockReturnValue(locationPromise);

    const { getByText, UNSAFE_getByType } = render(<StartDestinationPicker userLocation={userLocation} />);
    const currentLocationButton = getByText('Use Current Location');

    await act(async () => {
      fireEvent.press(currentLocationButton);
    });

    // Check if ActivityIndicator is shown
    const ActivityIndicator = require('react-native').ActivityIndicator;
    await waitFor(() => {
      expect(() => UNSAFE_getByType(ActivityIndicator)).not.toThrow();
    });

    // Resolve the promise
    await act(async () => {
      resolveLocation({
        coords: {
          latitude: 45.497,
          longitude: -73.579,
        }
      });
    });
  });

  it('passes value prop to BuildingSelector for start building', async () => {
    const { rerender } = render(<StartDestinationPicker userLocation={null} />);
    const onSelectStart = getStartSelector();
    const mockPlace = createMockPlace('Test Building', '123 Test St');

    await selectPlace(onSelectStart, mockPlace);

    rerender(<StartDestinationPicker userLocation={null} />);

    await waitFor(() => {
      const calls = (BuildingSelector as jest.Mock).mock.calls;
      const callWithValue = calls.find(call => call[0].value === 'Test Building');
      expect(callWithValue).toBeDefined();
    });
  });

  it('passes value prop to BuildingSelector for destination building', async () => {
    const { rerender } = render(<StartDestinationPicker userLocation={null} />);
    const onSelectDest = getDestinationSelector();
    const mockPlace = createMockPlace('Test Destination', '456 Test Ave', 41.8781, -87.6298);

    await selectPlace(onSelectDest, mockPlace);

    rerender(<StartDestinationPicker userLocation={null} />);

    await waitFor(() => {
      const calls = (BuildingSelector as jest.Mock).mock.calls;
      const callWithValue = calls.find(call => call[0].value === 'Test Destination');
      expect(callWithValue).toBeDefined();
    });
  });

  it('renders MaterialIcons for clear buttons', async () => {
    const { UNSAFE_getAllByType } = render(<StartDestinationPicker userLocation={null} />);
    const onSelectStart = getStartSelector();
    const mockPlace = createMockPlace('Test Building', '123 Test St');

    await selectPlace(onSelectStart, mockPlace);

    await waitFor(() => {
      const MaterialIcons = require('@expo/vector-icons').MaterialIcons;
      const icons = UNSAFE_getAllByType(MaterialIcons);
      expect(icons.length).toBeGreaterThan(0);
      const closeIcon = icons.find((icon: any) => icon.props.name === 'close');
      expect(closeIcon).toBeDefined();
    });
  });

  it('renders MaterialIcons for current location button', () => {
    const userLocation = { latitude: 45.5, longitude: -73.6 };
    const { UNSAFE_getAllByType } = render(<StartDestinationPicker userLocation={userLocation} />);
    
    const MaterialIcons = require('@expo/vector-icons').MaterialIcons;
    const icons = UNSAFE_getAllByType(MaterialIcons);
    // Check that my-location icon is present
    const locationIcon = icons.find((icon: any) => icon.props.name === 'my-location');
    expect(locationIcon).toBeDefined();
  });

  it('renders selected text with proper styling', async () => {
    const { getByText } = render(<StartDestinationPicker userLocation={null} />);
    const onSelectStart = getStartSelector();
    const mockPlace = createMockPlace('Styled Building', '789 Style Ave');

    await selectPlace(onSelectStart, mockPlace);

    await waitFor(() => {
      const selectedText = getByText('Selected: Styled Building');
      expect(selectedText).toBeTruthy();
      expect(selectedText.props.style).toBeDefined();
    });
  });

  it('renders all label texts', () => {
    const { getByText } = render(<StartDestinationPicker userLocation={null} />);
    
    expect(getByText('Start Building')).toBeTruthy();
    expect(getByText('Destination Building')).toBeTruthy();
  });
});
