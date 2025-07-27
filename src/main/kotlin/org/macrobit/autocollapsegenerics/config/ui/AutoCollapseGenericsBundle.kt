package org.macrobit.autocollapsegenerics

import com.intellij.DynamicBundle
import org.jetbrains.annotations.PropertyKey

object AutoCollapseGenericsBundle : DynamicBundle("messages.AutoCollapseGenericsBundle") {
  fun message(@PropertyKey(resourceBundle = "messages.AutoCollapseGenericsBundle") key: String, vararg params: Any): String {
    return getMessage(key, *params)
  }
}
