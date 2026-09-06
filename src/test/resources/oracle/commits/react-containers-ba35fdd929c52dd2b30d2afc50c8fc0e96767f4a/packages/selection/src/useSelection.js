/**
 * Copyright Zendesk, Inc.
 *
 * Use of this source code is governed under the Apache License, Version 2.0
 * found at http://www.apache.org/licenses/LICENSE-2.0.
 */

import { useEffect, useReducer } from 'react';

import {
  composeEventHandlers,
  getControlledValue,
  KEY_CODES
} from '@zendeskgarden/container-utilities';
import { DIRECTIONS } from './utils/DIRECTIONS';
import { ACTIONS } from './utils/ACTIONS';

function stateReducer(state, action) {
  switch (action.type) {
    case ACTIONS.FOCUS: {
      if (action.onFocus) {
        if (action.payload !== action.focusedItem) {
          action.onFocus(action.payload);
        }

        return state;
      }

      return { ...state, focusedItem: action.payload };
    }
    case ACTIONS.INCREMENT: {
      const controlledFocusedItem = getControlledValue(action.focusedItem, state.focusedItem);
      const controlledSelectedItem = getControlledValue(action.selectedItem, state.selectedItem);
      const currentItemIndex =
        controlledFocusedItem === undefined
          ? action.items.indexOf(controlledSelectedItem)
          : action.items.indexOf(controlledFocusedItem);
      const newFocusedItem = action.items[(currentItemIndex + 1) % action.items.length];

      if (action.onFocus) {
        action.onFocus(newFocusedItem);

        return state;
      }

      return { ...state, focusedItem: newFocusedItem };
    }
    case ACTIONS.DECREMENT: {
      const controlledFocusedItem = getControlledValue(action.focusedItem, state.focusedItem);
      const controlledSelectedItem = getControlledValue(action.selectedItem, state.selectedItem);
      const currentItemIndex =
        controlledFocusedItem === undefined
          ? action.items.indexOf(controlledSelectedItem)
          : action.items.indexOf(controlledFocusedItem);
      const newFocusedItem =
        action.items[(currentItemIndex + action.items.length - 1) % action.items.length];

      if (action.onFocus) {
        action.onFocus(newFocusedItem);

        return state;
      }

      return { ...state, focusedItem: newFocusedItem };
    }
    case ACTIONS.HOME: {
      if (action.onFocus) {
        action.onFocus(action.items[0]);

        return state;
      }

      return { ...state, focusedItem: action.items[0] };
    }
    case ACTIONS.END: {
      if (action.onFocus) {
        action.onFocus(action.items[action.items.length - 1]);

        return state;
      }

      return { ...state, focusedItem: action.items[action.items.length - 1] };
    }
    case ACTIONS.MOUSE_SELECT: {
      let isSelectControlled = false;
      let isFocusControlled = false;

      if (action.onSelect) {
        action.onSelect(action.payload);
        isSelectControlled = true;
      }

      if (action.onFocus) {
        action.onFocus(undefined);
        isFocusControlled = true;
      }

      if (isFocusControlled && isSelectControlled) {
        return state;
      }

      const updatedState = { ...state };

      if (!isSelectControlled) {
        updatedState.selectedItem = action.payload;
      }

      if (!isFocusControlled) {
        updatedState.focusedItem = undefined;
      }

      return updatedState;
    }
    case ACTIONS.KEYBOARD_SELECT: {
      if (action.onSelect) {
        action.onSelect(action.payload);

        return state;
      }

      return { ...state, selectedItem: action.payload };
    }
    case ACTIONS.EXIT_WIDGET: {
      if (action.onFocus) {
        action.onFocus(undefined);

        return state;
      }

      return { ...state, focusedItem: undefined };
    }
    default:
      return state;
  }
}

/**
 * Custom hook to manage selection using the Roving Tab Index strategy
 *
 * https://www.w3.org/TR/wai-aria-practices/#kbd_roving_tabindex
 */
export function useSelection({
  direction = DIRECTIONS.HORIZONTAL,
  defaultFocusedIndex = 0,
  defaultSelectedIndex,
  rtl,
  selectedItem,
  focusedItem,
  onSelect,
  onFocus
} = {}) {
  const refs = [];
  const items = [];

  const [state, dispatch] = useReducer(stateReducer, {
    selectedItem,
    focusedItem
  });

  const controlledFocusedItem = getControlledValue(focusedItem, state.focusedItem);
  const controlledSelectedItem = getControlledValue(selectedItem, state.selectedItem);

  useEffect(() => {
    if (controlledFocusedItem !== undefined) {
      const focusedIndex = items.indexOf(controlledFocusedItem);

      refs[focusedIndex] && refs[focusedIndex].current.focus();
    }
  }, [controlledFocusedItem]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (selectedItem === undefined && defaultSelectedIndex !== undefined) {
      dispatch({
        type: ACTIONS.KEYBOARD_SELECT,
        payload: items[defaultSelectedIndex],
        items,
        onSelect
      });
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const getContainerProps = ({ role = 'listbox', ...other } = {}) => ({
    role,
    'aria-orientation': direction === DIRECTIONS.BOTH ? undefined : direction,
    'data-garden-container-id': 'selection',
    'data-garden-container-version': PACKAGE_VERSION,
    ...other
  });

  const getItemProps = (
    {
      selectedAriaKey = 'aria-selected',
      role = 'option',
      onFocus: onFocusCallback,
      onKeyDown,
      onClick,
      item,
      focusRef,
      refKey = 'ref',
      ...other
    } = {},
    propGetterName = 'getItemProps'
  ) => {
    if (item === undefined) {
      throw new Error(
        `Accessibility Error: You must provide an "item" option to "${propGetterName}()"`
      );
    }

    if (focusRef === undefined) {
      throw new Error(
        `Accessibility Error: You must provide a "focusRef" option to "${propGetterName}()"`
      );
    }

    refs.push(focusRef);
    items.push(item);

    const isSelected = controlledSelectedItem === item;
    const isFocused =
      controlledFocusedItem === undefined ? isSelected : controlledFocusedItem === item;
    const tabIndex =
      isFocused ||
      (controlledSelectedItem === undefined &&
        controlledFocusedItem === undefined &&
        items.indexOf(item) === defaultFocusedIndex)
        ? 0
        : -1;
    const verticalDirection = direction === DIRECTIONS.VERTICAL || direction === DIRECTIONS.BOTH;
    const horizontalDirection =
      direction === DIRECTIONS.HORIZONTAL || direction === DIRECTIONS.BOTH;

    return {
      role,
      tabIndex,
      [selectedAriaKey]: isSelected,
      [refKey]: focusRef,
      onFocus: composeEventHandlers(onFocusCallback, () => {
        dispatch({ type: ACTIONS.FOCUS, payload: item, items, focusedItem, onFocus });
      }),
      onBlur: e => {
        if (e.target.tabIndex === 0) {
          dispatch({ type: ACTIONS.EXIT_WIDGET, items, onFocus });
        }
      },
      onClick: composeEventHandlers(onClick, () => {
        dispatch({ type: ACTIONS.MOUSE_SELECT, payload: item, items, onSelect, onFocus });
      }),
      onKeyDown: composeEventHandlers(onKeyDown, e => {
        if (
          (e.keyCode === KEY_CODES.UP && verticalDirection) ||
          (e.keyCode === KEY_CODES.LEFT && horizontalDirection)
        ) {
          if (rtl) {
            dispatch({ type: ACTIONS.INCREMENT, items, focusedItem, selectedItem, onFocus });
          } else {
            dispatch({ type: ACTIONS.DECREMENT, items, focusedItem, selectedItem, onFocus });
          }

          e.preventDefault();
        } else if (
          (e.keyCode === KEY_CODES.DOWN && verticalDirection) ||
          (e.keyCode === KEY_CODES.RIGHT && horizontalDirection)
        ) {
          if (rtl) {
            dispatch({ type: ACTIONS.DECREMENT, items, focusedItem, selectedItem, onFocus });
          } else {
            dispatch({ type: ACTIONS.INCREMENT, items, focusedItem, selectedItem, onFocus });
          }

          e.preventDefault();
        } else if (e.keyCode === KEY_CODES.HOME) {
          dispatch({ type: ACTIONS.HOME, items, onFocus });
          e.preventDefault();
        } else if (e.keyCode === KEY_CODES.END) {
          dispatch({ type: ACTIONS.END, items, onFocus });
          e.preventDefault();
        } else if (e.keyCode === KEY_CODES.SPACE || e.keyCode === KEY_CODES.ENTER) {
          dispatch({
            type: ACTIONS.KEYBOARD_SELECT,
            payload: item,
            items,
            onSelect
          });
          e.preventDefault();
        }
      }),
      ...other
    };
  };

  return {
    focusedItem: controlledFocusedItem,
    selectedItem: controlledSelectedItem,
    getItemProps,
    getContainerProps
  };
}
