package org.macrobit.autocollapsegenerics.config.ui

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import org.macrobit.autocollapsegenerics.AutoCollapseGenericsBundle
import org.macrobit.autocollapsegenerics.config.AutoCollapseGenericsNotifier
import org.macrobit.autocollapsegenerics.config.model.FoldingCondition
import org.macrobit.autocollapsegenerics.config.model.FoldingRule
import org.macrobit.autocollapsegenerics.config.model.FoldingTarget
import org.macrobit.autocollapsegenerics.config.storage.AutoCollapseGenericsSettings
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

private const val ENABLED_COLUMN = 0
private const val FOLDING_TARGET_COLUMN = 1
private const val FOLDING_CONDITION_COLUMN = 2
private const val MIN_GENERIC_COUNT_COLUMN = 3
private const val MIN_TEXT_LENGTH_COLUMN = 4

class AutoCollapseGenericsSettingsConfigurable : Configurable {
  private lateinit var mainPanel: JPanel
  private lateinit var enabledCheckBox: JBCheckBox
  private lateinit var foldingRulesTable: JTable

  private val settings = AutoCollapseGenericsSettings.Companion.getInstance()

  override fun getDisplayName(): String = AutoCollapseGenericsBundle.message("configurable.display.name")

  override fun createComponent(): JComponent {
    mainPanel = JPanel(BorderLayout())

    enabledCheckBox = JBCheckBox(AutoCollapseGenericsBundle.message("configurable.enabled"))
    enabledCheckBox.isSelected = settings.state.enabled
    mainPanel.add(enabledCheckBox, BorderLayout.NORTH)

    val columnNames = arrayOf(
      AutoCollapseGenericsBundle.message("foldingRule.enabled"),
      AutoCollapseGenericsBundle.message("foldingRule.target"),
      AutoCollapseGenericsBundle.message("foldingRule.condition"),
      AutoCollapseGenericsBundle.message("foldingRule.minGenericCount"),
      AutoCollapseGenericsBundle.message("foldingRule.minTextLength")
    )
    val foldingRulesByTarget = settings.state.foldingRulesByTarget

    val data = foldingRulesByTarget.map { (type, foldingRule) ->
      arrayOf(
        foldingRule.enabled,
        type,
        foldingRule.condition,
        foldingRule.minGenericCount,
        foldingRule.minTextLength
      )
    }.toTypedArray()

    val model = object : DefaultTableModel(data, columnNames) {
      override fun isCellEditable(row: Int, column: Int): Boolean {
        return enabledCheckBox.isSelected
      }

      override fun getColumnClass(columnIndex: Int): Class<*> {
        return when (columnIndex) {
          ENABLED_COLUMN -> java.lang.Boolean::class.java
          FOLDING_TARGET_COLUMN -> FoldingTarget::class.java
          FOLDING_CONDITION_COLUMN -> FoldingCondition::class.java
          MIN_GENERIC_COUNT_COLUMN, MIN_TEXT_LENGTH_COLUMN -> Integer::class.java
          else -> Any::class.java
        }
      }
    }

    foldingRulesTable = JBTable(model)
    customizeTableColumns()

    val scrollPane = JBScrollPane(foldingRulesTable)
    mainPanel.add(scrollPane, BorderLayout.CENTER)

    // Enable/disable table editing dynamically
    fun updateTableEnabledState() {
      foldingRulesTable.isEnabled = enabledCheckBox.isSelected
      foldingRulesTable.repaint()
    }

    enabledCheckBox.addActionListener {
      updateTableEnabledState()
    }

    updateTableEnabledState()

    return mainPanel
  }

  private fun customizeTableColumns() {
    foldingRulesTable.rowHeight = 24

    customizeEnabledColumn()
    customizeFoldingTargetColumn()
    customizeFoldingConditionColumn()
    customizeMinGenericCountColumn()
    customizeMinTextLengthColumn()
  }

  private fun customizeEnabledColumn() {
    val enabledColumn = foldingRulesTable.columnModel.getColumn(ENABLED_COLUMN)
    enabledColumn.maxWidth = 100
    enabledColumn.preferredWidth = 100
    enabledColumn.minWidth = 30
  }

  private fun customizeFoldingTargetColumn() {
    val foldingTargetColumn = foldingRulesTable.columnModel.getColumn(FOLDING_TARGET_COLUMN)
    foldingTargetColumn.cellRenderer = object : DefaultTableCellRenderer() {
      override fun setValue(value: Any?) {
        val target = value as? FoldingTarget
        text = AutoCollapseGenericsBundle.message("foldingTarget.${target}")
      }
    }
  }

  private fun customizeFoldingConditionColumn() {
    val foldingConditionColumn = foldingRulesTable.columnModel.getColumn(FOLDING_CONDITION_COLUMN)
    val foldingConditionComboBox = ComboBox(FoldingCondition.entries.toTypedArray(), 120)
    foldingConditionComboBox.setRenderer(object : ListCellRenderer<FoldingCondition> {
      private val defaultRenderer = DefaultListCellRenderer()
      override fun getListCellRendererComponent(
        list: JList<out FoldingCondition>,
        value: FoldingCondition?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
      ): Component {
        val comp = defaultRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
        if (comp is JLabel && value != null) {
          comp.text = AutoCollapseGenericsBundle.message("foldingCondition.${value}")
        }
        return comp
      }
    })
    foldingConditionColumn.cellEditor = DefaultCellEditor(foldingConditionComboBox)

    foldingConditionColumn.cellRenderer = object : DefaultTableCellRenderer() {
      override fun setValue(value: Any?) {
        val condition = value as? FoldingCondition
        text = AutoCollapseGenericsBundle.message("foldingCondition.${condition}")
      }
    }
  }

  private fun customizeMinGenericCountColumn() {
    foldingRulesTable.columnModel.getColumn(MIN_GENERIC_COUNT_COLUMN).cellEditor = SpinnerCellEditor()
  }

  private fun customizeMinTextLengthColumn() {
    foldingRulesTable.columnModel.getColumn(MIN_TEXT_LENGTH_COLUMN).cellEditor = SpinnerCellEditor()
  }

  override fun isModified(): Boolean {
    if (enabledCheckBox.isSelected != settings.state.enabled) return true

    val model = foldingRulesTable.model
    val foldingRulesByTarget = settings.state.foldingRulesByTarget
    for (row in 0 until model.rowCount) {
      val target = model.getValueAt(row, FOLDING_TARGET_COLUMN) as FoldingTarget
      val original = foldingRulesByTarget[target] ?: continue
      if (original.enabled != model.getValueAt(row, ENABLED_COLUMN) as Boolean) return true
      if (original.condition != model.getValueAt(row, FOLDING_CONDITION_COLUMN) as FoldingCondition) return true
      if (original.minGenericCount != model.getValueAt(row, MIN_GENERIC_COUNT_COLUMN) as Int) return true
      if (original.minTextLength != model.getValueAt(row, MIN_TEXT_LENGTH_COLUMN) as Int) return true
    }

    return false
  }

  override fun apply() {
    settings.state.enabled = enabledCheckBox.isSelected

    val model = foldingRulesTable.model
    val updated = mutableMapOf<FoldingTarget, FoldingRule>()
    for (row in 0 until model.rowCount) {
      val enabled = model.getValueAt(row, ENABLED_COLUMN) as Boolean
      val target = model.getValueAt(row, FOLDING_TARGET_COLUMN) as FoldingTarget
      val condition = model.getValueAt(row, FOLDING_CONDITION_COLUMN) as FoldingCondition
      val minGenericCount = model.getValueAt(row, MIN_GENERIC_COUNT_COLUMN) as Int
      val minTextLength = model.getValueAt(row, MIN_TEXT_LENGTH_COLUMN) as Int

      updated[target] = FoldingRule(enabled, condition, minGenericCount, minTextLength)
    }

    settings.state.foldingRulesByTarget = updated

    AutoCollapseGenericsNotifier.notifySettingsChanged()
  }

  override fun reset() {
    enabledCheckBox.isSelected = settings.state.enabled

    val foldingRulesByTarget = settings.state.foldingRulesByTarget
    val model = foldingRulesTable.model
    for (row in 0 until model.rowCount) {
      val target = model.getValueAt(row, FOLDING_TARGET_COLUMN) as FoldingTarget
      val current = foldingRulesByTarget[target] ?: continue
      model.setValueAt(current.enabled, row, ENABLED_COLUMN)
      model.setValueAt(current.condition, row, FOLDING_CONDITION_COLUMN)
      model.setValueAt(current.minGenericCount, row, MIN_GENERIC_COUNT_COLUMN)
      model.setValueAt(current.minTextLength, row, MIN_TEXT_LENGTH_COLUMN)
    }
  }

  override fun disposeUIResources() {}
}