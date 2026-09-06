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

interface IUseSelectionPropGetters<Item> {
  getContainerProps: <T>(options?: T) => T & React.HTMLProps<any>;
  getItemProps: <T extends IGetItemPropsOptions<Item>>(options: T, propGetterName?: string) => any;
}

export interface IUseSelectionState<Item> {
  focusedItem?: Item;
  selectedItem?: Item;
}

export interface IGetItemPropsOptions<Item> extends React.HTMLProps<any> {
  selectedAriaKey?: string;
  item: Item;
  focusRef: React.RefObject<any>;
  refKey?: string;
}

export type UseSelectionReturnValue<Item> = IUseSelectionState<Item> &
  IUseSelectionPropGetters<Item>;

export interface IUseSelectionProps<Item> {
  direction?: 'horizontal' | 'vertical' | 'both';
  defaultFocusedIndex?: number;
  defaultSelectedIndex?: number;
  rtl?: boolean;
  selectedItem?: Item;
  focusedItem?: Item;
  onSelect?: (selectedItem: Item) => void;
  onFocus?: (focusedItem?: Item) => void;
}

type onFocusCallback<Item> = (item?: Item) => void;
type onSelectCallback<Item> = (item?: Item) => void;

export type SELECTION_ACTION<Item> =
  | { type: 'FOCUS'; onFocus?: onFocusCallback<Item>; payload?: any; focusedItem?: any }
  | {
      type: 'INCREMENT';
      focusedItem?: any;
      selectedItem?: any;
      items: any[];
      onFocus?: onFocusCallback<Item>;
    }
  | {
      type: 'DECREMENT';
      focusedItem?: any;
      selectedItem?: any;
      items: any[];
      onFocus?: onFocusCallback<Item>;
    }
  | { type: 'HOME'; onFocus?: onFocusCallback<Item>; items: any[] }
  | { type: 'END'; onFocus?: onFocusCallback<Item>; items: any[] }
  | {
      type: 'MOUSE_SELECT';
      onSelect?: onSelectCallback<Item>;
      onFocus?: onFocusCallback<Item>;
      payload: any;
    }
  | { type: 'KEYBOARD_SELECT'; onSelect?: onSelectCallback<Item>; payload: any }
  | { type: 'EXIT_WIDGET'; onFocus?: onFocusCallback<Item> };

function stateReducer(state: IUseSelectionState<any>, action: SELECTION_ACTION<any>) {
  switch (action.type) {
    case 'FOCUS': {
      if (action.onFocus) {
        if (action.payload !== action.focusedItem) {
          action.onFocus(action.payload);
        }

        return state;
      }

      return { ...state, focusedItem: action.payload };
    }
    case 'INCREMENT': {
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
    case 'DECREMENT': {
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
    case 'HOME': {
      if (action.onFocus) {
        action.onFocus(action.items[0]);

        return state;
      }

      return { ...state, focusedItem: action.items[0] };
    }
    case 'END': {
      if (action.onFocus) {
        action.onFocus(action.items[action.items.length - 1]);

        return state;
      }

      return { ...state, focusedItem: action.items[action.items.length - 1] };
    }
    case 'MOUSE_SELECT': {
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
    case 'KEYBOARD_SELECT': {
      if (action.onSelect) {
        action.onSelect(action.payload);

        return state;
      }

      return { ...state, selectedItem: action.payload };
    }
    case 'EXIT_WIDGET': {
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
export function useSelection<Item = any>({
  direction = 'horizontal',
  defaultFocusedIndex = 0,
  defaultSelectedIndex,
  rtl,
  selectedItem,
  focusedItem,
  onSelect,
  onFocus
}: IUseSelectionProps<Item> = {}): UseSelectionReturnValue<Item> {
  const refs: React.MutableRefObject<any | null>[] = [];
  const items: Item[] = [];

  const [state, dispatch] = useReducer(stateReducer, {
    selectedItem,
    focusedItem
  });

  const controlledFocusedItem = getControlledValue(focusedItem, state.focusedItem);
  const controlledSelectedItem = getControlledValue(selectedItem, state.selectedItem);

  useEffect(() => {
    if (controlledFocusedItem !== undefined) {
      const focusedIndex = items.indexOf(controlledFocusedItem);

      refs[focusedIndex] && refs[focusedIndex].current!.focus();
    }
  }, [controlledFocusedItem]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (selectedItem === undefined && defaultSelectedIndex !== undefined) {
      dispatch({
        type: 'KEYBOARD_SELECT',
        payload: items[defaultSelectedIndex],
        onSelect
      });
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const getContainerProps = ({ role = 'listbox', ...other }: React.HTMLProps<any> = {}) =>
    ({
      role,
      'aria-orientation': direction === 'both' ? undefined : direction,
      'data-garden-container-id': 'selection',
      'data-garden-container-version': PACKAGE_VERSION,
      ...other
    } as any);

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
    }: IGetItemPropsOptions<Item> = {} as any,
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
    const verticalDirection = direction === 'vertical' || direction === 'both';
    const horizontalDirection = direction === 'horizontal' || direction === 'both';

    return {
      role,
      tabIndex,
      [selectedAriaKey]: isSelected,
      [refKey]: focusRef,
      onFocus: composeEventHandlers(onFocusCallback, () => {
        dispatch({ type: 'FOCUS', payload: item, focusedItem, onFocus });
      }),
      onBlur: (e: React.FocusEvent<HTMLElement>) => {
        if (e.target.tabIndex === 0) {
          dispatch({ type: 'EXIT_WIDGET', onFocus });
        }
      },
      onClick: composeEventHandlers(onClick, () => {
        dispatch({ type: 'MOUSE_SELECT', payload: item, onSelect, onFocus });
      }),
      onKeyDown: composeEventHandlers(onKeyDown, (e: React.KeyboardEvent) => {
        if (
          (e.keyCode === KEY_CODES.UP && verticalDirection) ||
          (e.keyCode === KEY_CODES.LEFT && horizontalDirection)
        ) {
          if (rtl) {
            dispatch({ type: 'INCREMENT', items, focusedItem, selectedItem, onFocus });
          } else {
            dispatch({ type: 'DECREMENT', items, focusedItem, selectedItem, onFocus });
          }

          e.preventDefault();
        } else if (
          (e.keyCode === KEY_CODES.DOWN && verticalDirection) ||
          (e.keyCode === KEY_CODES.RIGHT && horizontalDirection)
        ) {
          if (rtl) {
            dispatch({ type: 'DECREMENT', items, focusedItem, selectedItem, onFocus });
          } else {
            dispatch({ type: 'INCREMENT', items, focusedItem, selectedItem, onFocus });
          }

          e.preventDefault();
        } else if (e.keyCode === KEY_CODES.HOME) {
          dispatch({ type: 'HOME', items, onFocus });
          e.preventDefault();
        } else if (e.keyCode === KEY_CODES.END) {
          dispatch({ type: 'END', items, onFocus });
          e.preventDefault();
        } else if (e.keyCode === KEY_CODES.SPACE || e.keyCode === KEY_CODES.ENTER) {
          dispatch({
            type: 'KEYBOARD_SELECT',
            payload: item,
            onSelect
          });
          e.preventDefault();
        }
      }),
      ...other
    } as any;
  };

  return {
    focusedItem: controlledFocusedItem,
    selectedItem: controlledSelectedItem,
    getItemProps,
    getContainerProps
  };
}
