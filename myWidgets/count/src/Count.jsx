import { createElement } from "react";

import { CountButton } from "./components/Count";
import "./ui/Count.css";

export function Count({ intialValue }) {
    return <CountButton intialValue={intialValue} />;
}
