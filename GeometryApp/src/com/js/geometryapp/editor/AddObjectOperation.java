package com.js.geometryapp.editor;

import com.js.editor.UserEvent;
import com.js.editor.UserOperation;
import static com.js.basic.Tools.*;

/**
 * Operation for adding new objects
 */
public class AddObjectOperation extends UserOperation {

  public AddObjectOperation(Editor editor, EdObjectFactory factory) {
    mEditor = editor;
    mObjectFactory = factory;
  }

  @Override
  public void processUserEvent(UserEvent event) {
    switch (event.getCode()) {
    case UserEvent.CODE_DOWN:
      addNewObject(event);
      break;
    }
  }

  private void addNewObject(UserEvent event) {
    CommandForGeneralChanges c = new CommandForGeneralChanges(mEditor,
        mObjectFactory.getTag(), null);
    EdObjectArray objects = mEditor.objects();
    EdObject newObject = mObjectFactory.construct(event.getWorldLocation());
    newObject.setEditor(mEditor);
    int slot = objects.add(newObject);
    objects.setEditableSlot(slot);
    c.finish();
    event.clearOperation();
    UserOperation oper = newObject.buildEditOperation(slot, event);
    if (oper == null) {
      warning("unexpected failure to build edit operation");
      return;
    }
    event.setOperation(oper);
    oper.processUserEvent(event);
  }

  private Editor mEditor;
  private EdObjectFactory mObjectFactory;
}
