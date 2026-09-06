/* eslint-disable no-console */
/**
 * Copyright Zendesk, Inc.
 *
 * Use of this source code is governed under the Apache License, Version 2.0
 * found at http://www.apache.org/licenses/LICENSE-2.0.
 */

import React from 'react';
import { KEY_CODES } from '@zendeskgarden/container-utilities';
import { render, fireEvent } from '@testing-library/react';

import { SelectionContainer } from './SelectionContainer';

jest.useFakeTimers();

describe('SelectionContainer', () => {
  const itemValues = ['Item-1', 'Item-2', 'Item-3'];

  const BasicExample = ({
    direction,
    defaultFocusedIndex,
    defaultSelectedIndex,
    selectedAriaKey,
    rtl,
    onFocus,
    onSelect
  } = {}) => (
    <SelectionContainer
      direction={direction}
      defaultFocusedIndex={defaultFocusedIndex}
      defaultSelectedIndex={defaultSelectedIndex}
      rtl={rtl}
      onFocus={onFocus ? onFocus : undefined}
      onSelect={onSelect ? onSelect : undefined}
    >
      {({ getContainerProps, getItemProps, focusedItem, selectedItem }) => (
        <div {...getContainerProps({ 'data-test-id': 'container' })}>
          {itemValues.map(item => {
            const ref = React.createRef();
            const isSelected = item === selectedItem;
            const isFocused = item === focusedItem;

            return (
              <div
                {...getItemProps(
                  {
                    key: item,
                    item,
                    focusRef: ref,
                    selectedAriaKey,
                    'data-test-id': 'item',
                    'data-focused': isFocused,
                    'data-selected': isSelected
                  },
                  {
                    selectedAriaKey
                  }
                )}
              >
                {item}
              </div>
            );
          })}
        </div>
      )}
    </SelectionContainer>
  );

  const itemProps = props => {
    return render(
      <SelectionContainer>
        {({ getItemProps }) => <div {...getItemProps(props)}>Example</div>}
      </SelectionContainer>
    );
  };

  describe('controlled state', () => {
    describe('onFocus', () => {
      it('should call onFocus callback', () => {
        const onFocusSpy = jest.fn();
        const { getAllByTestId } = render(<BasicExample onFocus={onFocusSpy} />);
        const [item] = getAllByTestId('item');

        fireEvent.focus(item);

        expect(onFocusSpy).toHaveBeenCalledWith(itemValues[0]);
      });
    });

    describe('onSelect', () => {
      it('should call onSelect callback', () => {
        const onSelectSpy = jest.fn();
        const { getAllByTestId } = render(<BasicExample onSelect={onSelectSpy} />);
        const [item] = getAllByTestId('item');

        fireEvent.click(item);

        expect(onSelectSpy).toHaveBeenCalledWith(itemValues[0]);
      });
    });
  });

  describe('getContainerProps', () => {
    it('applies accessibility role', () => {
      const { getByTestId } = render(<BasicExample />);
      const container = getByTestId('container');

      expect(container).toHaveAttribute('role', 'listbox');
      expect(container).toHaveAttribute('aria-orientation', 'horizontal');
    });

    describe('while using vertical direction', () => {
      it('applies accessibility role', () => {
        const { getByTestId } = render(<BasicExample direction="vertical" />);

        expect(getByTestId('container')).toHaveAttribute('aria-orientation', 'vertical');
      });
    });

    it('first item in container defaults as the only initial focusable item', () => {
      const { getAllByTestId } = render(<BasicExample />);
      const [item] = getAllByTestId('item');

      fireEvent.focus(item);
      expect(item).toHaveAttribute('tabIndex', '0');
      expect(item).toHaveAttribute('data-focused', 'true');
    });

    describe('onFocus', () => {
      it('does not focus item if item is moused down', () => {
        const { getAllByTestId } = render(<BasicExample />);
        const [item] = getAllByTestId('item');

        fireEvent.click(item);
        expect(item).toHaveAttribute('data-focused', 'false');
        expect(item).toHaveAttribute('data-selected', 'true');
      });

      it('focuses first item if no item is currently selected', () => {
        const { getAllByTestId } = render(<BasicExample />);
        const [item] = getAllByTestId('item');

        fireEvent.focus(item);
        expect(item).toHaveAttribute('data-focused', 'true');
      });

      it('focuses last item if no item is currently selected and defaultFocusedIndex is provided', () => {
        const { getAllByTestId } = render(
          <BasicExample defaultFocusedIndex={itemValues.length - 1} />
        );
        const [, , item] = getAllByTestId('item');

        expect(item).toHaveAttribute('tabIndex', '0');
      });

      it('will focus currently selected item if available', () => {
        const { getAllByTestId } = render(<BasicExample />);
        const [, , item] = getAllByTestId('item');

        fireEvent.click(item);
        expect(item).toHaveAttribute('tabIndex', '0');
      });
    });

    describe('onBlur', () => {
      it('clears currently focused item', () => {
        const { getAllByTestId } = render(<BasicExample />);
        const [item] = getAllByTestId('item');

        fireEvent.focus(item);
        expect(item).toHaveAttribute('data-focused', 'true');

        fireEvent.blur(item);
        expect(item).toHaveAttribute('data-focused', 'false');
      });
    });

    describe('onKeyDown', () => {
      describe('ENTER keyCode', () => {
        it('selects currently focused item', () => {
          const { getAllByTestId } = render(<BasicExample />);
          const [item] = getAllByTestId('item');

          fireEvent.focus(item);
          fireEvent.keyDown(item, { keyCode: KEY_CODES.ENTER });
          expect(item).toHaveAttribute('data-selected', 'true');
        });
      });

      describe('SPACE keyCode', () => {
        it('selects currently focused item', () => {
          const { getAllByTestId } = render(<BasicExample />);
          const [item] = getAllByTestId('item');

          fireEvent.focus(item);
          fireEvent.keyDown(item, { keyCode: KEY_CODES.SPACE });
          expect(item).toHaveAttribute('data-selected', 'true');
        });
      });

      describe('HOME keyCode', () => {
        it('focuses first available item', () => {
          const { getAllByTestId } = render(<BasicExample />);
          const [item] = getAllByTestId('item');

          fireEvent.focus(item);
          fireEvent.keyDown(item, { keyCode: KEY_CODES.HOME });
          expect(item).toHaveAttribute('data-focused', 'true');
        });
      });

      describe('END keyCode', () => {
        it('focuses last available item', () => {
          const { getAllByTestId } = render(<BasicExample />);
          const [item, , lastItem] = getAllByTestId('item');

          fireEvent.focus(item);
          fireEvent.keyDown(item, { keyCode: KEY_CODES.END });
          expect(lastItem).toHaveAttribute('data-focused', 'true');
        });
      });

      describe('while in horizontal mode', () => {
        describe('LEFT keyCode', () => {
          describe('when dir is LTR', () => {
            it('decrements focusedIndex if currently greater than 0', () => {
              const { getAllByTestId } = render(<BasicExample />);
              const [item, secondItem] = getAllByTestId('item');

              fireEvent.focus(secondItem);
              fireEvent.keyDown(secondItem, { keyCode: KEY_CODES.LEFT });

              expect(item).toHaveAttribute('data-focused', 'true');
            });

            it('decrements and wraps focusedIndex if currently less than or equal to 0', () => {
              const { getAllByTestId } = render(<BasicExample />);
              const [item, , lastItem] = getAllByTestId('item');

              fireEvent.focus(item);
              fireEvent.keyDown(item, { keyCode: KEY_CODES.LEFT });
              expect(lastItem).toHaveAttribute('data-focused', 'true');
            });
          });

          describe('when dir is RTL', () => {
            it('increments focusedIndex if currently less than items length', () => {
              const { getAllByTestId } = render(<BasicExample rtl={true} />);
              const [item, secondItem] = getAllByTestId('item');

              fireEvent.focus(item);
              fireEvent.keyDown(item, { keyCode: KEY_CODES.LEFT });

              expect(secondItem).toHaveAttribute('data-focused', 'true');
            });

            it('increments and wraps focusedIndex if currently greater than or equal to items length', () => {
              const { getAllByTestId } = render(<BasicExample rtl={true} />);
              const [item, , lastItem] = getAllByTestId('item');

              fireEvent.focus(lastItem);
              fireEvent.keyDown(lastItem, { keyCode: KEY_CODES.LEFT });

              expect(item).toHaveAttribute('data-focused', 'true');
            });
          });
        });

        describe('RIGHT keyCode', () => {
          describe('when dir is LTR', () => {
            it('increments focusedIndex if currently less than items length', () => {
              const { getAllByTestId } = render(<BasicExample />);
              const [item, secondItem] = getAllByTestId('item');

              fireEvent.click(item);
              fireEvent.keyDown(item, { keyCode: KEY_CODES.RIGHT });
              expect(secondItem).toHaveAttribute('data-focused', 'true');
            });

            it('increments and wrap focusedIndex if currently greater than or equal to items length', () => {
              const { getAllByTestId } = render(<BasicExample />);
              const [item, , lastItem] = getAllByTestId('item');

              fireEvent.focus(lastItem);
              fireEvent.keyDown(lastItem, { keyCode: KEY_CODES.RIGHT });
              expect(item).toHaveAttribute('data-focused', 'true');
            });
          });

          describe('when dir is RTL', () => {
            it('decrements focusedIndex if currently greater than 0', () => {
              const { getAllByTestId } = render(<BasicExample rtl={true} />);
              const [item, secondItem] = getAllByTestId('item');

              fireEvent.focus(secondItem);
              fireEvent.keyDown(secondItem, { keyCode: KEY_CODES.RIGHT });

              expect(item).toHaveAttribute('data-focused', 'true');
            });

            it('decrements and wraps focusedIndex if currently 0', () => {
              const { getAllByTestId } = render(<BasicExample rtl={true} />);
              const [item, , lastItem] = getAllByTestId('item');

              fireEvent.focus(item);
              fireEvent.keyDown(item, { keyCode: KEY_CODES.RIGHT });

              expect(lastItem).toHaveAttribute('data-focused', 'true');
            });
          });
        });

        describe('UP keyCode', () => {
          it('does not perform any action', () => {
            const { getAllByTestId } = render(<BasicExample />);
            const [item, secondItem, lastItem] = getAllByTestId('item');

            fireEvent.click(item);
            fireEvent.keyDown(item, { keyCode: KEY_CODES.UP });

            expect(item).toHaveAttribute('data-focused', 'false');
            expect(secondItem).toHaveAttribute('data-focused', 'false');
            expect(lastItem).toHaveAttribute('data-focused', 'false');
          });
        });

        describe('DOWN keyCode', () => {
          it('does not perform any action', () => {
            const { getAllByTestId } = render(<BasicExample />);
            const [item, secondItem, lastItem] = getAllByTestId('item');

            fireEvent.click(item);
            fireEvent.keyDown(item, { keyCode: KEY_CODES.DOWN });

            expect(item).toHaveAttribute('data-focused', 'false');
            expect(secondItem).toHaveAttribute('data-focused', 'false');
            expect(lastItem).toHaveAttribute('data-focused', 'false');
          });
        });
      });

      describe('while using vertical direction', () => {
        describe('UP keyCode', () => {
          it('decrements focusedIndex if currently greater than 0', () => {
            const { getAllByTestId } = render(<BasicExample direction="vertical" />);
            const [item, secondItem] = getAllByTestId('item');

            fireEvent.click(secondItem);
            fireEvent.keyDown(secondItem, { keyCode: KEY_CODES.UP });

            expect(item).toHaveAttribute('data-focused', 'true');
          });

          it('decrements and wraps focusedIndex if currently less than or equal to 0', () => {
            const { getAllByTestId } = render(<BasicExample direction="vertical" />);
            const [item, , lastItem] = getAllByTestId('item');

            fireEvent.click(item);
            fireEvent.keyDown(item, { keyCode: KEY_CODES.UP });

            expect(lastItem).toHaveAttribute('data-focused', 'true');
          });
        });

        describe('DOWN keyCode', () => {
          it('increments focusedIndex if currently less than items length', () => {
            const { getAllByTestId } = render(<BasicExample direction="vertical" />);
            const [item, secondItem] = getAllByTestId('item');

            fireEvent.click(item);
            fireEvent.keyDown(item, { keyCode: KEY_CODES.DOWN });

            expect(secondItem).toHaveAttribute('data-focused', 'true');
          });

          it('increments and wraps focusedIndex if currently greater than or equal to items length', () => {
            const { getAllByTestId } = render(<BasicExample direction="vertical" />);
            const [item, , lastItem] = getAllByTestId('item');

            fireEvent.click(lastItem);
            fireEvent.keyDown(lastItem, { keyCode: KEY_CODES.DOWN });

            expect(item).toHaveAttribute('data-focused', 'true');
          });
        });

        describe('LEFT keyCode', () => {
          it('does not perform any action', () => {
            const { getAllByTestId } = render(<BasicExample direction="vertical" />);
            const [item, secondItem, lastItem] = getAllByTestId('item');

            fireEvent.click(item);
            fireEvent.keyDown(item, { keyCode: KEY_CODES.LEFT });

            expect(item).toHaveAttribute('data-focused', 'false');
            expect(secondItem).toHaveAttribute('data-focused', 'false');
            expect(lastItem).toHaveAttribute('data-focused', 'false');
          });
        });

        describe('RIGHT keyCode', () => {
          it('does not perform any action', () => {
            const { getAllByTestId } = render(<BasicExample direction="vertical" />);
            const [item, secondItem, lastItem] = getAllByTestId('item');

            fireEvent.click(item);
            fireEvent.keyDown(item, { keyCode: KEY_CODES.RIGHT });

            expect(item).toHaveAttribute('data-focused', 'false');
            expect(secondItem).toHaveAttribute('data-focused', 'false');
            expect(lastItem).toHaveAttribute('data-focused', 'false');
          });
        });
      });
    });
  });

  describe('getItemProps', () => {
    it('throws if no item is applied', () => {
      const originalError = console.error;

      console.error = jest.fn();

      expect(() => {
        itemProps();
      }).toThrow('Accessibility Error: You must provide an "item" option to "getItemProps()"');

      console.error = originalError;
    });

    it('throws if no focusRef prop is applied', () => {
      const originalError = console.error;

      console.error = jest.fn();

      expect(() => {
        itemProps({ item: 'example' });
      }).toThrow('Accessibility Error: You must provide a "focusRef" option to "getItemProps()"');

      console.error = originalError;
    });

    it('does not throw if item and focusRef props are applied', () => {
      expect(() => {
        itemProps({ item: 'example', focusRef: React.createRef() });
      }).not.toThrow();
    });

    it('applies accessibility role attribute', () => {
      const { getAllByTestId } = render(<BasicExample />);
      const [item] = getAllByTestId('item');

      expect(item).toHaveAttribute('role', 'option');
    });

    it('applies default selected aria value if none provided', () => {
      const { getAllByTestId } = render(<BasicExample />);
      const [item] = getAllByTestId('item');

      expect(item).toHaveAttribute('aria-selected', 'false');
    });

    it('applies selected aria value if defaultSelectedIndex is passed', () => {
      const { getAllByTestId } = render(<BasicExample defaultSelectedIndex={1} />);
      const [, item] = getAllByTestId('item');

      expect(item).toHaveAttribute('aria-selected', 'true');
    });

    it('applies custom selected aria value if provided', () => {
      const { getAllByTestId } = render(<BasicExample selectedAriaKey="aria-pressed" />);
      const [item] = getAllByTestId('item');

      expect(item).toHaveAttribute('aria-pressed', 'false');
    });

    describe('onClick', () => {
      it('should select item that was clicked', () => {
        const { getAllByTestId } = render(<BasicExample />);
        const [, , lastItem] = getAllByTestId('item');

        fireEvent.click(lastItem);

        expect(lastItem).toHaveAttribute('aria-selected', 'true');
      });

      it('should remove focus from all items', () => {
        const { getAllByTestId } = render(<BasicExample />);
        const [item, secondItem, lastItem] = getAllByTestId('item');

        fireEvent.click(lastItem);

        expect(item).toHaveAttribute('data-focused', 'false');
        expect(secondItem).toHaveAttribute('data-focused', 'false');
        expect(lastItem).toHaveAttribute('data-focused', 'false');
      });
    });
  });
});
