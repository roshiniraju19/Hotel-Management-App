import { createElement } from "react";

import { Component } from "react";

import { HelloWorldSample } from "./components/HelloWorldSample";
import "./ui/HeadingWidgets.css";

export class HeadingWidgets extends Component {
    render() {
        const { headingText, fontSize, fontStyle } = this.props;
        let style = {
            fontSize: `${fontSize}px`,
            fontStyle: fontStyle === "Italic" ? "italic" : "normal",
            fontWeight: fontStyle === "Bold" ? "bold" : "normal"
        };
        return <h1 style={style}>{headingText}</h1>;
    }
}