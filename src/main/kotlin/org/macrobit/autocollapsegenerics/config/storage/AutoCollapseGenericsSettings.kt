package org.macrobit.autocollapsegenerics.config.storage

import com.intellij.openapi.components.SerializablePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import org.macrobit.autocollapsegenerics.config.model.FoldingCondition
import org.macrobit.autocollapsegenerics.config.model.FoldingRule
import org.macrobit.autocollapsegenerics.config.model.FoldingTarget

@State(name = "AutoCollapseGenericsSettings", storages = [Storage("autoCollapseGenerics.xml")])
class AutoCollapseGenericsSettings : SerializablePersistentStateComponent<AutoCollapseGenericsSettings.State>(State()) {

  class State {
    var enabled: Boolean = true

    var foldingRulesByTarget: MutableMap<FoldingTarget, FoldingRule> = FoldingTarget.entries.associateWith {
      FoldingRule(
        minGenericCount = 1,
        minTextLength = 15,
        condition = FoldingCondition.GENERIC_COUNT,
        enabled = true,
      )
    }.toMutableMap()
  }

  override fun loadState(state: State) {
    // Fix: ensure all enum values are present even if missing from the XML

    val existing = state.foldingRulesByTarget
    val newMap = LinkedHashMap<FoldingTarget, FoldingRule>()

    for (target in FoldingTarget.entries) {
      if (target == FoldingTarget.METHOD_RETURN) {
        // Insert missing keys before METHOD_RETURN
        for (entry in FoldingTarget.entries) {
          if (entry != FoldingTarget.METHOD_RETURN &&
            entry !in existing
          ) {
            newMap[entry] = FoldingRule(
              minGenericCount = 1,
              minTextLength = 15,
              condition = FoldingCondition.GENERIC_COUNT,
              enabled = true,
            )
          }
        }
      }

      if (target in existing) {
        newMap[target] = existing[target]!!
      } else if (target !in newMap) {
        newMap[target] = FoldingRule(
          minGenericCount = 1,
          minTextLength = 15,
          condition = FoldingCondition.GENERIC_COUNT,
          enabled = true,
        )
      }
    }

    state.foldingRulesByTarget = newMap
    super.loadState(state)
  }

  companion object {
    fun getInstance(): AutoCollapseGenericsSettings = service()
  }
}