import { createElement , useState} from "react";

export function CountButton({ intialValue }) {
    const [count, setCount] = useState(intialValue);
    const increment = () => {
        setCount(prevState => prevState + 1);
    }; 

    const decrement = () => {
        setCount(prevState => prevState - 1);
    };
    
    return (
        <div>
            <p>{count}</p>
            <button onClick={increment}>+</button>
            <button onClick={decrement}>-</button>
        </div>
    );
}
