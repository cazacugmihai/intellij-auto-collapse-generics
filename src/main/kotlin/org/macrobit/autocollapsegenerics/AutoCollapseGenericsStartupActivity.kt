package org.macrobit.autocollapsegenerics

import com.intellij.openapi.editor.*
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import org.macrobit.autocollapsegenerics.config.model.FoldingCondition
import org.macrobit.autocollapsegenerics.config.model.FoldingRule
import org.macrobit.autocollapsegenerics.config.model.FoldingTarget
import org.macrobit.autocollapsegenerics.config.storage.AutoCollapseGenericsSettings

class AutoCollapseGenericsStartupActivity : ProjectActivity {

  override suspend fun execute(project: Project) {
    EditorFactory.getInstance().addEditorFactoryListener(object : EditorFactoryListener {
      override fun editorCreated(event: EditorFactoryEvent) {
        reprocessEditor(event)
      }
    }, project)
  }

  fun reprocessEditor(event: EditorFactoryEvent) {
    val editor: Editor = event.editor
    val project = editor.project ?: return
    val document = editor.document

    val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document) as? PsiJavaFile ?: return

    val foldingModel: FoldingModel = editor.foldingModel
    val classes: List<PsiClass> = PsiTreeUtil.getChildrenOfTypeAsList(psiFile, PsiClass::class.java)

    val foldingRulesByTarget = AutoCollapseGenericsSettings.getInstance().state.foldingRulesByTarget
    val pluginEnabled = AutoCollapseGenericsSettings.getInstance().state.enabled

    foldingModel.runBatchFoldingOperation {
      foldingModel.allFoldRegions.filter { it.placeholderText == "<...>" }.forEach { foldingModel.removeFoldRegion(it) }

      if (!pluginEnabled) return@runBatchFoldingOperation

      for (psiClass in classes) {
        tryFoldClass(foldingRulesByTarget, psiClass, document, foldingModel)
        tryFoldExtends(foldingRulesByTarget, psiClass, document, foldingModel)
        tryFoldImplements(foldingRulesByTarget, psiClass, document, foldingModel)
        tryFoldFields(foldingRulesByTarget, psiClass, document, foldingModel)
        tryFoldMethods(psiClass, foldingRulesByTarget, document, foldingModel)
      }
    }
  }

  private fun tryFoldClass(
    foldingRulesByTarget: Map<FoldingTarget, FoldingRule>,
    psiClass: PsiClass,
    document: Document,
    foldingModel: FoldingModel
  ) {
    foldingRulesByTarget[FoldingTarget.CLASS_TYPE_PARAMETER]?.takeIf { it.enabled }?.let { foldingRule ->
      psiClass.typeParameterList?.let {
        tryFold(document, foldingModel, it.textRange.startOffset, it.textRange.endOffset, foldingRule)
      }
    }
  }

  private fun tryFoldExtends(
    foldingRulesByTarget: Map<FoldingTarget, FoldingRule>,
    psiClass: PsiClass,
    document: Document,
    foldingModel: FoldingModel
  ) {
    foldingRulesByTarget[FoldingTarget.EXTENDS]?.takeIf { it.enabled }?.let { foldingRule ->
      psiClass.extendsList?.referenceElements?.forEach { ref ->
        ref.parameterList?.let {
          tryFold(document, foldingModel, it.textRange.startOffset, it.textRange.endOffset, foldingRule)
        }
      }
    }
  }

  private fun tryFoldImplements(
    foldingRulesByTarget: Map<FoldingTarget, FoldingRule>,
    psiClass: PsiClass,
    document: Document,
    foldingModel: FoldingModel
  ) {
    foldingRulesByTarget[FoldingTarget.IMPLEMENTS]?.takeIf { it.enabled }?.let { foldingRule ->
      psiClass.implementsList?.referenceElements?.forEach { ref ->
        ref.parameterList?.let {
          tryFold(document, foldingModel, it.textRange.startOffset, it.textRange.endOffset, foldingRule)
        }
      }
    }
  }

  private fun tryFoldFields(
    foldingRulesByTarget: Map<FoldingTarget, FoldingRule>,
    psiClass: PsiClass,
    document: Document,
    foldingModel: FoldingModel
  ) {
    foldingRulesByTarget[FoldingTarget.FIELD]?.takeIf { it.enabled }?.let { foldingRule ->
      psiClass.fields.forEach { field ->
        val typeElement = field.typeElement
        typeElement?.children?.forEach { child ->
          if (child is PsiJavaCodeReferenceElement) {
            child.parameterList?.let { pl ->
              tryFold(document, foldingModel, pl.textRange.startOffset, pl.textRange.endOffset, foldingRule)
            }
          }
        }
      }
    }
  }

  private fun tryFoldMethods(
    psiClass: PsiClass,
    foldingRulesByTarget: Map<FoldingTarget, FoldingRule>,
    document: Document,
    foldingModel: FoldingModel
  ) {
    psiClass.methods.forEach { method ->
      tryFoldMethodsReturnTypeGenerics(foldingRulesByTarget, method, document, foldingModel)
      tryFoldMethodParametersGenerics(foldingRulesByTarget, method, document, foldingModel)
    }
  }

  private fun tryFoldMethodsReturnTypeGenerics(
    foldingRulesByTarget: Map<FoldingTarget, FoldingRule>,
    method: PsiMethod,
    document: Document,
    foldingModel: FoldingModel
  ) {
    foldingRulesByTarget[FoldingTarget.METHOD_RETURN]?.takeIf { it.enabled }?.let { foldingRule ->
      val returnTypeElement = method.returnTypeElement
      if (returnTypeElement != null) {
        PsiTreeUtil.findChildOfType(returnTypeElement, PsiJavaCodeReferenceElement::class.java)?.let { ref ->
          ref.parameterList?.let {
            tryFold(document, foldingModel, it.textRange.startOffset, it.textRange.endOffset, foldingRule)
          }
        }
      }
    }
  }

  private fun tryFoldMethodParametersGenerics(
    foldingRulesByTarget: Map<FoldingTarget, FoldingRule>,
    method: PsiMethod,
    document: Document,
    foldingModel: FoldingModel
  ) {
    foldingRulesByTarget[FoldingTarget.METHOD_PARAMETER]?.takeIf { it.enabled }?.let { foldingRule ->
      method.parameterList.parameters.forEach { param ->
        param.typeElement?.innermostComponentReferenceElement?.parameterList?.let { pl ->
          tryFold(document, foldingModel, pl.textRange.startOffset, pl.textRange.endOffset, foldingRule)
        }
      }
    }
  }

  private fun tryFold(document: Document, foldingModel: FoldingModel, start: Int, end: Int, foldingRule: FoldingRule) {
    val contentLength = end - start
    if (contentLength < 3) return

    val shouldFold = when (foldingRule.condition) {
      FoldingCondition.TEXT_LENGTH -> contentLength >= foldingRule.minTextLength
      FoldingCondition.GENERIC_COUNT -> {
        val text = document.text.substring(start, end)
        val count = text.count { it == ',' } + 1
        count >= foldingRule.minGenericCount
      }
    }

    if (shouldFold) {
      val region: FoldRegion? = foldingModel.addFoldRegion(start, end, "<...>")
      region?.isExpanded = false
    }
  }

}