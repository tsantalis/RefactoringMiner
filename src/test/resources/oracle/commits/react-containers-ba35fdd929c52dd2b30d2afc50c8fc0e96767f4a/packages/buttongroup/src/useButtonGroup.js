/**
 * Copyright Zendesk, Inc.
 *
 * Use of this source code is governed under the Apache License, Version 2.0
 * found at http://www.apache.org/licenses/LICENSE-2.0.
 */

import { useSelection } from '@zendeskgarden/container-selection';

export function useButtonGroup(options) {
  const { selectedItem, focusedItem, getContainerProps, getItemProps } = useSelection(options);

  const getGroupProps = ({ role = 'group', ...other } = {}) => {
    return {
      role,
      'data-garden-container-id': 'buttongroup',
      'data-garden-container-version': PACKAGE_VERSION,
      ...other
    };
  };

  const getButtonProps = ({
    role = 'button',
    key,
    selectedAriaKey = 'aria-pressed',
    ...other
  } = {}) => {
    return {
      role,
      key,
      selectedAriaKey,
      ...other
    };
  };

  return {
    selectedItem,
    focusedItem,
    getGroupProps: props => getContainerProps(getGroupProps(props)),
    getButtonProps: props => getItemProps(getButtonProps(props), 'getButtonProps')
  };
}
