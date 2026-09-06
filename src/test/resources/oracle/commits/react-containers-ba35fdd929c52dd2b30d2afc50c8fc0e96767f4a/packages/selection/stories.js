/**
 * Copyright Zendesk, Inc.
 *
 * Use of this source code is governed under the Apache License, Version 2.0
 * found at http://www.apache.org/licenses/LICENSE-2.0.
 */

import React from 'react';

import { storiesOf } from '@storybook/react';
import { withKnobs, select, boolean, number } from '@storybook/addon-knobs';

import { SelectionContainer, useSelection } from './src';

import { DIRECTIONS } from './src/utils/DIRECTIONS';

storiesOf('Selection Containers', module)
  .addDecorator(withKnobs)
  .add('useSelection', () => {
    const items = ['One', 'Two', 'Three'];
    const isRtl = boolean('Enable RTL', false);

    const Selection = ({ direction }) => {
      const { focusedItem, selectedItem, getContainerProps, getItemProps } = useSelection({
        direction,
        defaultSelectedIndex: number('defaultSelectedIndex', 0),
        rtl: isRtl
      });

      return (
        <ul {...getContainerProps()} style={{ display: 'flex' }}>
          {items.map(item => {
            const itemRef = React.createRef();
            const isSelected = selectedItem === item;
            const isFocused = focusedItem === item;

            return (
              <li
                {...getItemProps({
                  key: item,
                  item,
                  focusRef: itemRef
                })}
                style={{
                  listStyle: 'none',
                  margin: 16,
                  padding: 8,
                  textAlign: 'center'
                }}
              >
                {item}
                {isSelected && <div>[Selected]</div>}
                {isFocused && <div>(Focused)</div>}
              </li>
            );
          })}
        </ul>
      );
    };

    return (
      <Selection
        direction={select(
          'Direction',
          {
            vertical: DIRECTIONS.VERTICAL,
            horizontal: DIRECTIONS.HORIZONTAL,
            both: DIRECTIONS.BOTH
          },
          DIRECTIONS.HORIZONTAL
        )}
      />
    );
  })
  .add('SelectionContainer', () => {
    const items = ['Item 1', 'Item 2', 'Item 3'];

    return (
      <SelectionContainer
        direction="vertical"
        defaultFocusedIndex={number('defaultFocusedIndex', 0)}
      >
        {({ selectedItem, focusedItem, getContainerProps, getItemProps }) => (
          <ul {...getContainerProps()}>
            {items.map(item => {
              const ref = React.createRef();
              const isSelected = item === selectedItem;
              const isFocused = item === focusedItem;

              return (
                <li
                  {...getItemProps({
                    key: item,
                    item,
                    focusRef: ref
                  })}
                >
                  {item}
                  {isSelected && <span> - Selected</span>}
                  {isFocused && <span> - Focused</span>}
                </li>
              );
            })}
          </ul>
        )}
      </SelectionContainer>
    );
  });
