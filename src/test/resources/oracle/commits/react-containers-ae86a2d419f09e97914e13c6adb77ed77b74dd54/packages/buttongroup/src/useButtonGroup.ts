/**
 * Copyright Zendesk, Inc.
 *
 * Use of this source code is governed under the Apache License, Version 2.0
 * found at http://www.apache.org/licenses/LICENSE-2.0.
 */

import {
  useSelection,
  IUseSelectionProps,
  IUseSelectionState,
  IGetItemPropsOptions
} from '@zendeskgarden/container-selection';

// eslint-disable-next-line @typescript-eslint/no-empty-interface
export interface IUseButtonGroupProps<Item> extends IUseSelectionProps<Item> {}

export interface IUseButtonGroupPropGetters<Item> {
  getGroupProps: <T>(options?: T) => T & React.HTMLProps<any>;
  getButtonProps: <T extends IGetItemPropsOptions<Item>>(
    options: T,
    propGetterName?: string
  ) => any;
}

export type UseButtonGroupReturnValue<Item> = IUseSelectionState<Item> &
  IUseButtonGroupPropGetters<Item>;

export function useButtonGroup<Item = any>(
  options: IUseButtonGroupProps<Item>
): UseButtonGroupReturnValue<Item> {
  const { selectedItem, focusedItem, getContainerProps, getItemProps } = useSelection<Item>(
    options
  );

  const getGroupProps = ({ role = 'group', ...other } = {}) => {
    return {
      role,
      'data-garden-container-id': 'buttongroup',
      'data-garden-container-version': PACKAGE_VERSION,
      ...other
    };
  };

  const getButtonProps = ({ role = 'button', selectedAriaKey = 'aria-pressed', ...other } = {}) => {
    return {
      role,
      selectedAriaKey,
      ...other
    };
  };

  return {
    selectedItem,
    focusedItem,
    getGroupProps: props => getContainerProps(getGroupProps(props) as any),
    getButtonProps: props => getItemProps(getButtonProps(props) as any, 'getButtonProps')
  };
}
