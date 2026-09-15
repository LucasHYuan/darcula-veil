package com.lucashyuan.darculaveil

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.TitledSeparator
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import com.lucashyuan.darculaveil.palette.VeilPaletteProviders
import com.lucashyuan.darculaveil.style.VeilStyleStrategies
import javax.swing.JComponent
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

class VeilConfigurable : Configurable {

    private val styleModeBox = ComboBox(VeilStyleStrategies.displayNames().toTypedArray())
    private val paletteSourceBox = ComboBox(VeilPaletteProviders.displayNames().toTypedArray())
    private val paletteStepsSpinner = buildSpinner(VeilSettings.MIN_PALETTE_STEPS, VeilSettings.MAX_PALETTE_STEPS)
    private val paletteSaturationSpinner = buildPercentSpinner()
    private val invertSpinner = buildPercentSpinner()
    private val hueRotateSpinner = buildSpinner(VeilSettings.MIN_HUE_DEGREES, VeilSettings.MAX_HUE_DEGREES)
    private val saturateSpinner = buildPercentSpinner()
    private val brightnessSpinner = buildPercentSpinner()
    private val contrastSpinner = buildPercentSpinner()
    private val revertMediaCheckBox = JBCheckBox("Restore original colors on images and video")

    override fun getDisplayName(): String = "Darcula Veil"

    override fun createComponent(): JComponent {
        return FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Style mode:"), styleModeBox, 1, false)
            .addComponent(TitledSeparator("Palette Quantization"))
            .addLabeledComponent(JBLabel("Palette source:"), paletteSourceBox, 1, false)
            .addLabeledComponent(JBLabel("Palette steps:"), paletteStepsSpinner, 1, false)
            .addLabeledComponent(JBLabel("Pre-quantize saturation (%):"), paletteSaturationSpinner, 1, false)
            .addComponentToRightColumn(JBLabel("<html>Quantization applies to the whole document, including images and video.<br>An ancestor SVG filter cannot be undone by descendants.</html>"))
            .addComponent(TitledSeparator("Continuous Filter"))
            .addLabeledComponent(JBLabel("Invert (%):"), invertSpinner, 1, false)
            .addLabeledComponent(JBLabel("Hue rotate (deg):"), hueRotateSpinner, 1, false)
            .addLabeledComponent(JBLabel("Saturate (%):"), saturateSpinner, 1, false)
            .addLabeledComponent(JBLabel("Brightness (%):"), brightnessSpinner, 1, false)
            .addLabeledComponent(JBLabel("Contrast (%):"), contrastSpinner, 1, false)
            .addComponent(revertMediaCheckBox)
            .addComponentFillVertically(JBLabel(""), 0)
            .panel
    }

    override fun isModified(): Boolean {
        val settings = VeilSettings.state()

        return selectedStyleModeId() != settings.styleModeId
            || selectedPaletteSourceId() != settings.paletteSourceId
            || value(paletteStepsSpinner) != settings.paletteSteps
            || value(paletteSaturationSpinner) != settings.paletteSourceSaturationPercent
            || value(invertSpinner) != settings.invertPercent
            || value(hueRotateSpinner) != settings.hueRotateDegrees
            || value(saturateSpinner) != settings.saturatePercent
            || value(brightnessSpinner) != settings.brightnessPercent
            || value(contrastSpinner) != settings.contrastPercent
            || revertMediaCheckBox.isSelected != settings.revertMedia
    }

    override fun apply() {
        val settings = VeilSettings.state()
        settings.styleModeId = selectedStyleModeId()
        settings.paletteSourceId = selectedPaletteSourceId()
        settings.paletteSteps = value(paletteStepsSpinner)
        settings.paletteSourceSaturationPercent = value(paletteSaturationSpinner)
        settings.invertPercent = value(invertSpinner)
        settings.hueRotateDegrees = value(hueRotateSpinner)
        settings.saturatePercent = value(saturateSpinner)
        settings.brightnessPercent = value(brightnessSpinner)
        settings.contrastPercent = value(contrastSpinner)
        settings.revertMedia = revertMediaCheckBox.isSelected

        ApplicationManager.getApplication().messageBus.syncPublisher(VeilStateListener.TOPIC).veilStateChanged()
    }

    override fun reset() {
        val settings = VeilSettings.state()
        styleModeBox.selectedItem = VeilStyleStrategies.byId(settings.styleModeId).displayName
        paletteSourceBox.selectedItem = VeilPaletteProviders.byId(settings.paletteSourceId).displayName
        paletteStepsSpinner.value = settings.paletteSteps
        paletteSaturationSpinner.value = settings.paletteSourceSaturationPercent
        invertSpinner.value = settings.invertPercent
        hueRotateSpinner.value = settings.hueRotateDegrees
        saturateSpinner.value = settings.saturatePercent
        brightnessSpinner.value = settings.brightnessPercent
        contrastSpinner.value = settings.contrastPercent
        revertMediaCheckBox.isSelected = settings.revertMedia
    }

    private fun selectedStyleModeId(): String = VeilStyleStrategies.byDisplayName(styleModeBox.selectedItem as String).id

    private fun selectedPaletteSourceId(): String = VeilPaletteProviders.byDisplayName(paletteSourceBox.selectedItem as String).id

    private fun value(spinner: JSpinner): Int = (spinner.value as Number).toInt()

    private fun buildPercentSpinner(): JSpinner = buildSpinner(VeilSettings.MIN_PERCENT, VeilSettings.MAX_PERCENT)

    private fun buildSpinner(minimum: Int, maximum: Int): JSpinner = JSpinner(SpinnerNumberModel(minimum, minimum, maximum, 1))
}
