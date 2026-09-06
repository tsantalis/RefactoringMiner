/**
 * Copyright Zendesk, Inc.
 *
 * Use of this source code is governed under the Apache License, Version 2.0
 * found at http://www.apache.org/licenses/LICENSE-2.0.
 */

import React, { createRef, useState } from 'react';

import { storiesOf } from '@storybook/react';
import { withKnobs } from '@storybook/addon-knobs';

import { ButtonGroupContainer, useButtonGroup } from './src';

const buttons = ['Button 1', 'Button 2', 'Button 3'];
const buttonRefs = buttons.map(() => createRef(null));

storiesOf('ButtonGroup Container', module)
  .addDecorator(withKnobs)
  .add('useButtonGroup', () => {
    const ButtonGroup = () => {
      const [controlledSelectedItem, setSelectedItem] = useState();
      const { selectedItem, focusedItem, getButtonProps, getGroupProps } = useButtonGroup({
        selectedItem: controlledSelectedItem,
        onSelect: newSelectedItem => setSelectedItem(newSelectedItem)
      });

      return (
        <div {...getGroupProps()}>
          {buttons.map((button, index) => (
            <button
              {...getButtonProps({
                key: button,
                item: button,
                focusRef: buttonRefs[index],
                style: {
                  boxShadow: button === focusedItem && 'inset 0 0 0 3px rgba(31,115,183, 0.35)',
                  outline: 'none',
                  color: button === selectedItem ? '#fff' : '#1f73b7',
                  background: button === selectedItem && '#144a75',
                  padding: '10px'
                }
              })}
            >
              {button}
            </button>
          ))}
        </div>
      );
    };

    return <ButtonGroup />;
  })
  .add('ButtonGroupContainer', () => (
    <ButtonGroupContainer>
      {({ selectedItem, focusedItem, getButtonProps, getGroupProps }) => (
        <div {...getGroupProps()}>
          {buttons.map((button, index) => (
            <button
              {...getButtonProps({
                key: button,
                item: button,
                focusRef: buttonRefs[index],
                style: {
                  boxShadow: button === focusedItem && 'inset 0 0 0 3px rgba(31,115,183, 0.35)',
                  outline: 'none',
                  color: button === selectedItem ? '#fff' : '#1f73b7',
                  background: button === selectedItem && '#144a75',
                  padding: '10px'
                }
              })}
            >
              {button}
            </button>
          ))}
        </div>
      )}
    </ButtonGroupContainer>
  ));
