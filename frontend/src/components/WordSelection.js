import React, {useState} from "react";
import "../styles/GameSetup.css";
import avatar from "./assets/avatar.png"
import starIcon from "./assets/wordSelectionPage/star.png"
import crayons from "./assets/setupPage/crayons.png"
import hourglass from "./assets/setupPage/hourglass.png"
import right_bkg from "./assets/right_bkg.png";
import left_bkg from "./assets/left_bkg.png";

const words = ["Cat", "Dog", "Horse", "Hamster", "Table", "Chair", "Sofa", "Bed", "Pizza", "Burger", "Apple", "Cake"];
const getRandomWords = () => {
  return words
    .sort(() => 0.5 - Math.random()) //Shuffle array
    .slice(0, 4); //Pick first 4 words
};
const WordSelection = () => {
  const [randomWords, setRandomWords] = useState(getRandomWords());
  return (
    <div className="setup_container">
      <img src={left_bkg} alt="Left Top" className="left-top-image" />
      <img src={right_bkg} alt="Right Top" className="right-top-image" />
      <div className="setup_logo"><h1>InkBlink</h1></div>
      <hr class="underline"/>
      <h2 className="setup_join_title">Your Turn...</h2>
      <div className="ws_content_container">
        <div className="ws_player_card">
          <img src={avatar} alt="User-Avatar" className="user-avatar"/>
          <div>
            <div className="ws_username">Jerry</div>
            <div className="ws_points">0 points</div>
          </div>
        </div>
        <div className="setup_image">
           <img src={crayons} alt="crayons" className="setup_image crayons" />
          <img src={hourglass} alt="hourglass" className="setup_image hourglass" />
        </div>
        <div className="ws_box">
          <div className="setup_join_title"> Select one of the four words to draw</div>
          <hr class="ws_underline"/>
         <div className="ws_button_group">
              {randomWords.map((word, index) => (
                <button key={index} className="ws_btn">{word}</button>
              ))}
           </div>
        </div> 
      </div>
    </div>
  );
};
export default WordSelection;
