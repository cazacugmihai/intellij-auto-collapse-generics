package org.macrobit.autocollapsegenerics.config

import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.EditorFactoryEvent
import org.macrobit.autocollapsegenerics.AutoCollapseGenericsStartupActivity

object AutoCollapseGenericsNotifier {

  fun notifySettingsChanged() {
    val factory = EditorFactory.getInstance()
    for (editor in factory.allEditors) {
      editor.project?.let {
        val event = EditorFactoryEvent(factory, editor)
        AutoCollapseGenericsStartupActivity().reprocessEditor(event)
      }
    }
  }

}