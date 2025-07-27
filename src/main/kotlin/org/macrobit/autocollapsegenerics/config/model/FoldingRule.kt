package org.macrobit.autocollapsegenerics.config.model

data class FoldingRule(
  var enabled: Boolean = true,
  var condition: FoldingCondition = FoldingCondition.GENERIC_COUNT,
  var minGenericCount: Int = 1,
  var minTextLength: Int = 15
)