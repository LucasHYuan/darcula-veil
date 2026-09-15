package com.lucashyuan.darculaveil

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.TitledSeparator
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.lucashyuan.darculaveil.palette.VeilPaletteProviders
import com.lucashyuan.darculaveil.style.PaletteStyleStrategy
import com.lucashyuan.darculaveil.style.VeilStyleStrategies
import javax.swing.JComponent
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel
import javax.swing.event.DocumentEvent

class VeilConfigurable : Configurable {

    private val styleModeBox = ComboBox(VeilStyleStrategies.displayNames().toTypedArray())
    private val paletteSourceBox = ComboBox(VeilPaletteProviders.displayNames().toTypedArray())
    private val paletteStepsSpinner = buildSpinner(VeilSettings.MIN_PALETTE_STEPS, VeilSettings.MAX_PALETTE_STEPS)
    private val paletteFloorSpinner = buildSpinner(0, 100)
    private val paletteCeilingSpinner = buildSpinner(0, 100)
    private val paletteSaturationSpinner = buildPercentSpinner()
    private val paletteReversedCheckBox = JBCheckBox("Reverse mapping (bright page content becomes dark)")
    private val customPaletteField = JBTextField()
    private val palettePreview = VeilPalettePreview { PaletteStyleStrategy.buildPalette(buildUiState()) }

    private val invertSpinner = buildPercentSpinner()
    private val hueRotateSpinner = buildSpinner(VeilSettings.MIN_HUE_DEGREES, VeilSettings.MAX_HUE_DEGREES)
    private val saturateSpinner = buildPercentSpinner()
    private val brightnessSpinner = buildPercentSpinner()
    private val contrastSpinner = buildPercentSpinner()
    private val revertMediaCheckBox = JBCheckBox("Restore original colors on images and video")

    private val nativeLayeredCheckBox = JBCheckBox("Blend the embedded window with the IDE background (WS_EX_LAYERED)")
    private val nativeOpacitySpinner = buildSpinner(VeilSettings.MIN_NATIVE_OPACITY_PERCENT, VeilSettings.MAX_NATIVE_OPACITY_PERCENT)

    override fun getDisplayName(): String = "Darcula Veil"

    override fun createComponent(): JComponent {
        installPreviewListeners()

        return FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Style mode:"), styleModeBox, 1, false)
            .addComponent(TitledSeparator("Palette Quantization"))
            .addLabeledComponent(JBLabel("Palette source:"), paletteSourceBox, 1, false)
            .addLabeledComponent(JBLabel("Custom colors (hex, comma separated):"), customPaletteField, 1, false)
            .addLabeledComponent(JBLabel("Palette steps:"), paletteStepsSpinner, 1, false)
            .addLabeledComponent(JBLabel("Range floor (%):"), paletteFloorSpinner, 1, false)
            .addLabeledComponent(JBLabel("Range ceiling (%):"), paletteCeilingSpinner, 1, false)
            .addLabeledComponent(JBLabel("Pre-quantize saturation (%):"), paletteSaturationSpinner, 1, false)
            .addComponent(paletteReversedCheckBox)
            .addLabeledComponent(JBLabel("Resulting palette:"), palettePreview, 1, false)
            .addComponentToRightColumn(JBLabel("<html>Leftmost swatch is what a white page background becomes.<br>Quantization covers images and video as well; an ancestor SVG filter<br>cannot be undone by descendants.</html>"))
            .addComponent(TitledSeparator("Continuous Filter"))
            .addLabeledComponent(JBLabel("Invert (%):"), invertSpinner, 1, false)
            .addLabeledComponent(JBLabel("Hue rotate (deg):"), hueRotateSpinner, 1, false)
            .addLabeledComponent(JBLabel("Saturate (%):"), saturateSpinner, 1, false)
            .addLabeledComponent(JBLabel("Brightness (%):"), brightnessSpinner, 1, false)
            .addLabeledComponent(JBLabel("Contrast (%):"), contrastSpinner, 1, false)
            .addComponent(revertMediaCheckBox)
            .addComponent(TitledSeparator("Native Window Embedding"))
            .addComponent(nativeLayeredCheckBox)
            .addLabeledComponent(JBLabel("Window opacity (%):"), nativeOpacitySpinner, 1, false)
            .addComponentToRightColumn(JBLabel("<html>Making a foreign window layered can break or slow down D3D rendering.<br>Leave it off unless the blend is worth it. Applies on Resync Geometry.</html>"))
            .addComponentFillVertically(JBLabel(""), 0)
            .panel
    }

    override fun isModified(): Boolean {
        val settings = VeilSettings.state()

        return selectedStyleModeId() != settings.styleModeId
            || selectedPaletteSourceId() != settings.paletteSourceId
            || customPaletteField.text != settings.customPaletteHex
            || value(paletteStepsSpinner) != settings.paletteSteps
            || value(paletteFloorSpinner) != settings.paletteFloorPercent
            || value(paletteCeilingSpinner) != settings.paletteCeilingPercent
            || value(paletteSaturationSpinner) != settings.paletteSourceSaturationPercent
            || paletteReversedCheckBox.isSelected != settings.paletteReversed
            || value(invertSpinner) != settings.invertPercent
            || value(hueRotateSpinner) != settings.hueRotateDegrees
            || value(saturateSpinner) != settings.saturatePercent
            || value(brightnessSpinner) != settings.brightnessPercent
            || value(contrastSpinner) != settings.contrastPercent
            || revertMediaCheckBox.isSelected != settings.revertMedia
            || nativeLayeredCheckBox.isSelected != settings.nativeLayeredEnabled
            || value(nativeOpacitySpinner) != settings.nativeOpacityPercent
    }

    override fun apply() {
        val settings = VeilSettings.state()
        settings.styleModeId = selectedStyleModeId()
        settings.paletteSourceId = selectedPaletteSourceId()
        settings.customPaletteHex = customPaletteField.text
        settings.paletteSteps = value(paletteStepsSpinner)
        settings.paletteFloorPercent = value(paletteFloorSpinner)
        settings.paletteCeilingPercent = value(paletteCeilingSpinner)
        settings.paletteSourceSaturationPercent = value(paletteSaturationSpinner)
        settings.paletteReversed = paletteReversedCheckBox.isSelected
        settings.invertPercent = value(invertSpinner)
        settings.hueRotateDegrees = value(hueRotateSpinner)
        settings.saturatePercent = value(saturateSpinner)
        settings.brightnessPercent = value(brightnessSpinner)
        settings.contrastPercent = value(contrastSpinner)
        settings.revertMedia = revertMediaCheckBox.isSelected
        settings.nativeLayeredEnabled = nativeLayeredCheckBox.isSelected
        settings.nativeOpacityPercent = value(nativeOpacitySpinner)

        ApplicationManager.getApplication().messageBus.syncPublisher(VeilStateListener.TOPIC).veilStateChanged()
    }

    override fun reset() {
        val settings = VeilSettings.state()
        styleModeBox.selectedItem = VeilStyleStrategies.byId(settings.styleModeId).displayName
        paletteSourceBox.selectedItem = VeilPaletteProviders.byId(settings.paletteSourceId).displayName
        customPaletteField.text = settings.customPaletteHex
        paletteStepsSpinner.value = settings.paletteSteps
        paletteFloorSpinner.value = settings.paletteFloorPercent
        paletteCeilingSpinner.value = settings.paletteCeilingPercent
        paletteSaturationSpinner.value = settings.paletteSourceSaturationPercent
        paletteReversedCheckBox.isSelected = settings.paletteReversed
        invertSpinner.value = settings.invertPercent
        hueRotateSpinner.value = settings.hueRotateDegrees
        saturateSpinner.value = settings.saturatePercent
        brightnessSpinner.value = settings.brightnessPercent
        contrastSpinner.value = settings.contrastPercent
        revertMediaCheckBox.isSelected = settings.revertMedia
        nativeLayeredCheckBox.isSelected = settings.nativeLayeredEnabled
        nativeOpacitySpinner.value = settings.nativeOpacityPercent
        palettePreview.repaint()
    }

    private fun installPreviewListeners() {
        listOf(paletteStepsSpinner, paletteFloorSpinner, paletteCeilingSpinner).forEach { spinner ->
            spinner.addChangeListener { palettePreview.repaint() }
        }

        paletteSourceBox.addActionListener { palettePreview.repaint() }
        paletteReversedCheckBox.addActionListener { palettePreview.repaint() }

        customPaletteField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(event: DocumentEvent) {
                palettePreview.repaint()
            }
        })
    }

    private fun buildUiState(): VeilSettings.State {
        val state = VeilSettings.State()
        state.paletteSourceId = selectedPaletteSourceId()
        state.customPaletteHex = customPaletteField.text
        state.paletteSteps = value(paletteStepsSpinner)
        state.paletteFloorPercent = value(paletteFloorSpinner)
        state.paletteCeilingPercent = value(paletteCeilingSpinner)
        state.paletteReversed = paletteReversedCheckBox.isSelected

        return state
    }

    private fun selectedStyleModeId(): String = VeilStyleStrategies.byDisplayName(styleModeBox.selectedItem as String).id

    private fun selectedPaletteSourceId(): String = VeilPaletteProviders.byDisplayName(paletteSourceBox.selectedItem as String).id

    private fun value(spinner: JSpinner): Int = (spinner.value as Number).toInt()

    private fun buildPercentSpinner(): JSpinner = buildSpinner(VeilSettings.MIN_PERCENT, VeilSettings.MAX_PERCENT)

    private fun buildSpinner(minimum: Int, maximum: Int): JSpinner = JSpinner(SpinnerNumberModel(minimum, minimum, maximum, 1))
}
