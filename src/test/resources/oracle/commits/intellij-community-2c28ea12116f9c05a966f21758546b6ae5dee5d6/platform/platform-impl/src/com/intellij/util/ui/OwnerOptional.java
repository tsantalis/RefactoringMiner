// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.util.ui;

import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.IdePopupManager;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public final class OwnerOptional {
  private static Window findOwnerByComponent(Component component) {
    if (component == null) component = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
    if (component == null) {
      component = Window.getWindows()[0];
    }
    return (component instanceof Window) ? (Window) component : SwingUtilities.getWindowAncestor(component);
  }

  private Window myPermanentOwner;

  private OwnerOptional(Window permanentOwner) {
    this.myPermanentOwner = permanentOwner;
  }

  public static OwnerOptional fromComponent (Component parentComponent) {
    Window owner = findOwnerByComponent(parentComponent);

    IdePopupManager manager = IdeEventQueue.getInstance().getPopupManager();

    if (manager.isPopupWindow(owner)) {
      if (!owner.isFocused() || !SystemInfo.isJetBrainsJvm) {
        do {
          owner = owner.getOwner();
        }
        while (UIUtil.isSimpleWindow(owner));
      }
    }

    if (owner instanceof Dialog ownerDialog) {
      if (!ownerDialog.isModal() && !UIUtil.isPossibleOwner(ownerDialog)) {
        while (owner instanceof Dialog && !((Dialog)owner).isModal()) {
          owner = owner.getOwner();
        }
      }
    }

    while (owner != null && !owner.isShowing()) {
      owner = owner.getOwner();
    }

    // `Window` cannot be a parent of `JDialog`
    if (UIUtil.isSimpleWindow(owner)) {
      owner = null;
    }

    return new OwnerOptional(owner);
  }

  public OwnerOptional ifDialog(Consumer<? super Dialog> consumer) {
    if (myPermanentOwner instanceof Dialog) {
      consumer.accept((Dialog)myPermanentOwner);
    }
    return this;
  }

  public OwnerOptional ifNull(Consumer<? super Frame> consumer) {
    if (myPermanentOwner == null) {
      consumer.accept(null);
    }
    return this;
  }

  public OwnerOptional ifFrame(Consumer<? super Frame> consumer) {
    if (myPermanentOwner instanceof Frame) {
      if (myPermanentOwner instanceof IdeFrame.Child ideFrameChild) {
        myPermanentOwner = WindowManager.getInstance().getFrame(ideFrameChild.getProject());
      }
      consumer.accept((Frame)this.myPermanentOwner);
    }
    return this;
  }

  public Window get() {
    return myPermanentOwner;
  }
}
