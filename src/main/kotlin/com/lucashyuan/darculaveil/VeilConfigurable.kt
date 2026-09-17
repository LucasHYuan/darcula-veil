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
import com.lucashyuan.darculaveil.pageturn.VeilPageTurnStrategies
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
    private val webVignetteSpinner = buildSpinner(0, 100)
    private val webVignetteCenterSpinner = buildSpinner(0, 95)
    private val customPaletteField = JBTextField()
    private val palettePreview = VeilPalettePreview { PaletteStyleStrategy.buildPalette(buildUiState()) }

    private val invertSpinner = buildPercentSpinner()
    private val hueRotateSpinner = buildSpinner(VeilSettings.MIN_HUE_DEGREES, VeilSettings.MAX_HUE_DEGREES)
    private val saturateSpinner = buildPercentSpinner()
    private val brightnessSpinner = buildPercentSpinner()
    private val contrastSpinner = buildPercentSpinner()
    private val revertMediaCheckBox = JBCheckBox("Restore original colors on images and video")

    private val pageTurnStrategyBox = ComboBox(VeilPageTurnStrategies.displayNames().toTypedArray())
    private val pageTurnScrollSpinner = buildSpinner(VeilSettings.MIN_PAGE_TURN_SCROLL_PERCENT, 100)
    private val pageTurnCooldownSpinner = buildSpinner(0, VeilSettings.MAX_PAGE_TURN_COOLDOWN_MS)
    private val pageTurnKeysCheckBox = JBCheckBox("Handle page turn keys inside the page")
    private val keymapBridgeCheckBox = JBCheckBox("Route IDE keymap shortcuts into the focused browser")
    private val pageTurnWheelMirrorCheckBox = JBCheckBox("Also mirror wheel shortcuts inside the page (usually redundant)")
    private val pageTurnForwardKeyField = JBTextField()
    private val pageTurnBackwardKeyField = JBTextField()
    private val pageTurnForwardSelectorField = JBTextField()
    private val pageTurnBackwardSelectorField = JBTextField()

    private val nativeFocusTransferCheckBox = JBCheckBox("Transfer keyboard focus on click (AttachThreadInput)")
    private val nativeLayeredCheckBox = JBCheckBox("Also make the target window itself translucent (WS_EX_LAYERED)")
    private val nativeOpacitySpinner = buildSpinner(VeilSettings.MIN_NATIVE_OPACITY_PERCENT, VeilSettings.MAX_NATIVE_OPACITY_PERCENT)
    private val overlayEnabledCheckBox = JBCheckBox("Composite a themed overlay on top of the embedded window")
    private val overlayTintSpinner = buildSpinner(0, 100)
    private val overlayVignetteSpinner = buildSpinner(0, 100)
    private val overlayScanlineSpacingSpinner = buildSpinner(0, VeilSettings.MAX_SCANLINE_SPACING)
    private val overlayScanlineSpinner = buildSpinner(0, 100)

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
            .addLabeledComponent(JBLabel("Edge vignette (%, 0 = off):"), webVignetteSpinner, 1, false)
            .addLabeledComponent(JBLabel("Vignette clear radius (%):"), webVignetteCenterSpinner, 1, false)
            .addComponentToRightColumn(JBLabel("<html>The vignette dissolves the rectangular page boundary into the tool window.<br>Enabling it moves the quantize filter from html to body so the vignette itself<br>stays unfiltered; set it to 0 to go back to filtering html directly.</html>"))
            .addLabeledComponent(JBLabel("Resulting palette:"), palettePreview, 1, false)
            .addComponentToRightColumn(JBLabel("<html>Leftmost swatch is what a white page background becomes.<br>Quantization covers images and video as well; an ancestor SVG filter<br>cannot be undone by descendants.</html>"))
            .addComponent(TitledSeparator("Continuous Filter"))
            .addLabeledComponent(JBLabel("Invert (%):"), invertSpinner, 1, false)
            .addLabeledComponent(JBLabel("Hue rotate (deg):"), hueRotateSpinner, 1, false)
            .addLabeledComponent(JBLabel("Saturate (%):"), saturateSpinner, 1, false)
            .addLabeledComponent(JBLabel("Brightness (%):"), brightnessSpinner, 1, false)
            .addLabeledComponent(JBLabel("Contrast (%):"), contrastSpinner, 1, false)
            .addComponent(revertMediaCheckBox)
            .addComponent(TitledSeparator("Page Turning"))
            .addLabeledComponent(JBLabel("Turn mode:"), pageTurnStrategyBox, 1, false)
            .addLabeledComponent(JBLabel("Scroll amount (% of viewport):"), pageTurnScrollSpinner, 1, false)
            .addLabeledComponent(JBLabel("Cooldown between turns (ms, 0 = off):"), pageTurnCooldownSpinner, 1, false)
            .addLabeledComponent(JBLabel("Next page CSS selector:"), pageTurnForwardSelectorField, 1, false)
            .addLabeledComponent(JBLabel("Previous page CSS selector:"), pageTurnBackwardSelectorField, 1, false)
            .addComponent(pageTurnKeysCheckBox)
            .addComponent(keymapBridgeCheckBox)
            .addComponent(pageTurnWheelMirrorCheckBox)
            .addLabeledComponent(JBLabel("Forward key (KeyboardEvent.key):"), pageTurnForwardKeyField, 1, false)
            .addLabeledComponent(JBLabel("Backward key (KeyboardEvent.key):"), pageTurnBackwardKeyField, 1, false)
            .addComponentToRightColumn(JBLabel("<html>Keys are handled by a listener injected into the page, because a focused<br>JCEF window consumes keystrokes before the IDE action system sees them.<br>DarculaVeil.PageForward / PageBackward are also available in Keymap.</html>"))
            .addComponent(TitledSeparator("Native Window Embedding"))
            .addComponent(overlayEnabledCheckBox)
            .addLabeledComponent(JBLabel("Overlay tint (%):"), overlayTintSpinner, 1, false)
            .addLabeledComponent(JBLabel("Overlay vignette (%):"), overlayVignetteSpinner, 1, false)
            .addLabeledComponent(JBLabel("Scanline spacing (px, 0 = off):"), overlayScanlineSpacingSpinner, 1, false)
            .addLabeledComponent(JBLabel("Scanline strength (%):"), overlayScanlineSpinner, 1, false)
            .addComponentToRightColumn(JBLabel("<html>The overlay is a layered click-through child window composited above the target.<br>It can only add pixels, not read them: tint, vignette and scanlines work,<br>contrast or palette remapping do not.</html>"))
            .addComponent(nativeFocusTransferCheckBox)
            .addComponentToRightColumn(JBLabel("<html>Attaching input queues across processes is the most dangerous call in this plugin.<br>Turn it off if the IDE ever stops responding to input while a window is embedded.</html>"))
            .addComponent(nativeLayeredCheckBox)
            .addLabeledComponent(JBLabel("Target window opacity (%):"), nativeOpacitySpinner, 1, false)
            .addComponentToRightColumn(JBLabel("<html>Making a foreign window layered can break or slow down D3D rendering.<br>Applies on Resync Geometry.</html>"))
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
            || value(webVignetteSpinner) != settings.webVignettePercent
            || value(webVignetteCenterSpinner) != settings.webVignetteCenterPercent
            || value(invertSpinner) != settings.invertPercent
            || value(hueRotateSpinner) != settings.hueRotateDegrees
            || value(saturateSpinner) != settings.saturatePercent
            || value(brightnessSpinner) != settings.brightnessPercent
            || value(contrastSpinner) != settings.contrastPercent
            || revertMediaCheckBox.isSelected != settings.revertMedia
            || selectedPageTurnStrategyId() != settings.pageTurnStrategyId
            || value(pageTurnScrollSpinner) != settings.pageTurnScrollPercent
            || value(pageTurnCooldownSpinner) != settings.pageTurnCooldownMs
            || pageTurnKeysCheckBox.isSelected != settings.pageTurnKeysEnabled
            || keymapBridgeCheckBox.isSelected != settings.keymapBridgeEnabled
            || pageTurnWheelMirrorCheckBox.isSelected != settings.pageTurnWheelMirrorEnabled
            || pageTurnForwardKeyField.text != settings.pageTurnForwardKey
            || pageTurnBackwardKeyField.text != settings.pageTurnBackwardKey
            || pageTurnForwardSelectorField.text != settings.pageTurnForwardSelector
            || pageTurnBackwardSelectorField.text != settings.pageTurnBackwardSelector
            || nativeFocusTransferCheckBox.isSelected != settings.nativeFocusTransferEnabled
            || nativeLayeredCheckBox.isSelected != settings.nativeLayeredEnabled
            || value(nativeOpacitySpinner) != settings.nativeOpacityPercent
            || overlayEnabledCheckBox.isSelected != settings.overlayEnabled
            || value(overlayTintSpinner) != settings.overlayTintPercent
            || value(overlayVignetteSpinner) != settings.overlayVignettePercent
            || value(overlayScanlineSpacingSpinner) != settings.overlayScanlineSpacing
            || value(overlayScanlineSpinner) != settings.overlayScanlinePercent
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
        settings.webVignettePercent = value(webVignetteSpinner)
        settings.webVignetteCenterPercent = value(webVignetteCenterSpinner)
        settings.invertPercent = value(invertSpinner)
        settings.hueRotateDegrees = value(hueRotateSpinner)
        settings.saturatePercent = value(saturateSpinner)
        settings.brightnessPercent = value(brightnessSpinner)
        settings.contrastPercent = value(contrastSpinner)
        settings.revertMedia = revertMediaCheckBox.isSelected
        settings.pageTurnStrategyId = selectedPageTurnStrategyId()
        settings.pageTurnScrollPercent = value(pageTurnScrollSpinner)
        settings.pageTurnCooldownMs = value(pageTurnCooldownSpinner)
        settings.pageTurnKeysEnabled = pageTurnKeysCheckBox.isSelected
        settings.keymapBridgeEnabled = keymapBridgeCheckBox.isSelected
        settings.pageTurnWheelMirrorEnabled = pageTurnWheelMirrorCheckBox.isSelected
        settings.pageTurnForwardKey = pageTurnForwardKeyField.text
        settings.pageTurnBackwardKey = pageTurnBackwardKeyField.text
        settings.pageTurnForwardSelector = pageTurnForwardSelectorField.text
        settings.pageTurnBackwardSelector = pageTurnBackwardSelectorField.text
        settings.nativeFocusTransferEnabled = nativeFocusTransferCheckBox.isSelected
        settings.nativeLayeredEnabled = nativeLayeredCheckBox.isSelected
        settings.nativeOpacityPercent = value(nativeOpacitySpinner)
        settings.overlayEnabled = overlayEnabledCheckBox.isSelected
        settings.overlayTintPercent = value(overlayTintSpinner)
        settings.overlayVignettePercent = value(overlayVignetteSpinner)
        settings.overlayScanlineSpacing = value(overlayScanlineSpacingSpinner)
        settings.overlayScanlinePercent = value(overlayScanlineSpinner)

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
        webVignetteSpinner.value = settings.webVignettePercent
        webVignetteCenterSpinner.value = settings.webVignetteCenterPercent
        invertSpinner.value = settings.invertPercent
        hueRotateSpinner.value = settings.hueRotateDegrees
        saturateSpinner.value = settings.saturatePercent
        brightnessSpinner.value = settings.brightnessPercent
        contrastSpinner.value = settings.contrastPercent
        revertMediaCheckBox.isSelected = settings.revertMedia
        pageTurnStrategyBox.selectedItem = VeilPageTurnStrategies.byId(settings.pageTurnStrategyId).displayName
        pageTurnScrollSpinner.value = settings.pageTurnScrollPercent
        pageTurnCooldownSpinner.value = settings.pageTurnCooldownMs
        pageTurnKeysCheckBox.isSelected = settings.pageTurnKeysEnabled
        keymapBridgeCheckBox.isSelected = settings.keymapBridgeEnabled
        pageTurnWheelMirrorCheckBox.isSelected = settings.pageTurnWheelMirrorEnabled
        pageTurnForwardKeyField.text = settings.pageTurnForwardKey
        pageTurnBackwardKeyField.text = settings.pageTurnBackwardKey
        pageTurnForwardSelectorField.text = settings.pageTurnForwardSelector
        pageTurnBackwardSelectorField.text = settings.pageTurnBackwardSelector
        nativeFocusTransferCheckBox.isSelected = settings.nativeFocusTransferEnabled
        nativeLayeredCheckBox.isSelected = settings.nativeLayeredEnabled
        nativeOpacitySpinner.value = settings.nativeOpacityPercent
        overlayEnabledCheckBox.isSelected = settings.overlayEnabled
        overlayTintSpinner.value = settings.overlayTintPercent
        overlayVignetteSpinner.value = settings.overlayVignettePercent
        overlayScanlineSpacingSpinner.value = settings.overlayScanlineSpacing
        overlayScanlineSpinner.value = settings.overlayScanlinePercent
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

    private fun selectedPageTurnStrategyId(): String = VeilPageTurnStrategies.byDisplayName(pageTurnStrategyBox.selectedItem as String).id

    private fun selectedPaletteSourceId(): String = VeilPaletteProviders.byDisplayName(paletteSourceBox.selectedItem as String).id

    private fun value(spinner: JSpinner): Int = (spinner.value as Number).toInt()

    private fun buildPercentSpinner(): JSpinner = buildSpinner(VeilSettings.MIN_PERCENT, VeilSettings.MAX_PERCENT)

    private fun buildSpinner(minimum: Int, maximum: Int): JSpinner = JSpinner(SpinnerNumberModel(minimum, minimum, maximum, 1))
}
