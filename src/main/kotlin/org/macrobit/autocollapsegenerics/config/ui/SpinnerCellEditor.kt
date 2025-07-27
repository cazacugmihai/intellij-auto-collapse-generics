package org.macrobit.autocollapsegenerics.config.ui

import java.awt.Component
import javax.swing.AbstractCellEditor
import javax.swing.JSpinner
import javax.swing.JTable
import javax.swing.SpinnerNumberModel
import javax.swing.table.TableCellEditor

class SpinnerCellEditor(val defaultValue: Int = 1, val minValue: Int = 1, val maxValue: Int = 10_000) :
  AbstractCellEditor(), TableCellEditor {

  private val spinner: JSpinner = JSpinner(SpinnerNumberModel(defaultValue, minValue, maxValue, 1))

  override fun getTableCellEditorComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    row: Int,
    column: Int
  ): Component {
    spinner.value = (value as? Int)?.coerceIn(minValue, maxValue) ?: defaultValue
    return spinner
  }

  override fun getCellEditorValue(): Any {
    try {
      // Force commit to capture manually typed value
      val editor = spinner.editor
      if (editor is JSpinner.DefaultEditor) {
        editor.commitEdit()
      }
    } catch (e: java.text.ParseException) {
      // Optional: reset to last valid value or show a warning
    }
    return spinner.value
  }
}
