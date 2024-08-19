import { Component, createElement } from "react";

import { HelloWorldSample } from "./components/HelloWorldSample";
import "./ui/HeadingWidget.css";

export function HeadingWidget({ sampleText }) {
    return <HelloWorldSample sampleText={sampleText} />;
}

// import { Component, createElement } from "react";

// export class HeadingWidget extends Component {
//     render() {
//         const { headingSize, font, text } = this.props;

//         const headingStyle = {
//             fontSize: headingSize,
//             fontFamily: font,
//         };

//         return (
//             <div style={headingStyle}>
//                 {text}
//             </div>
//         );
//     }
// }
