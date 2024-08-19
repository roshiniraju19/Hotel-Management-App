import { createElement } from "react";



export function preview(props) {
    const { headingText, fontSize, fontStyle } = props;
    let style = {
        fontSize: `${fontSize}px`,
        fontStyle: fontStyle === "Italic" ? "italic" : "normal",
        fontWeight: fontStyle === "Bold" ? "bold" : "normal"
    };
    return <h1 style={style}>{headingText}</h1>;
}

export function getPreviewCss() {
    return require("./ui/HeadingWidgets.css");
}
