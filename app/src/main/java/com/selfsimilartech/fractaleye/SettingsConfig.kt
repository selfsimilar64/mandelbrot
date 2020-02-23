package com.selfsimilartech.fractaleye

class SettingsConfig (
        var resolution               : Resolution      = Resolution.FULL,
        var precision                : Precision       = Precision.SINGLE,
        var autoPrecision            : Boolean         = true,
        var continuousRender         : Boolean         = false,
        var displayParams            : Boolean         = false,
        var showProgress             : Boolean         = true,
        var sampleOnStrictTranslate  : Boolean         = false,
        var renderBackground         : Boolean         = true,
        var fitToViewport            : Boolean         = false,
        var hideNavBar               : Boolean         = true,
        var showHints                : Boolean         = true,
        var colorListViewType        : ListLayoutType  = ListLayoutType.GRID,
        var shapeListViewType        : ListLayoutType  = ListLayoutType.GRID,
        var textureListViewType      : ListLayoutType  = ListLayoutType.GRID
)