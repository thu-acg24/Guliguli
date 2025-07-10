// DanmakuInput.tsx
import React, { useState, useEffect, useRef } from 'react';
import { useUserToken } from "Globals/GlobalStore";
import { PublishDanmakuMessage } from "Plugins/DanmakuService/APIs/PublishDanmakuMessage";
import "./DanmakuInput.css";
import Danmaku from 'danmaku';

const DanmakuInput: React.FC<{
  videoID: number;
  isLoggedIn: boolean;
  setShowLoginModal: (value: boolean) => void;
  currentTime: number;
  danmakuRef: React.RefObject<Danmaku>;
}> = ({
  videoID,
  isLoggedIn,
  setShowLoginModal,
  currentTime,
  danmakuRef
}) => {
  const userToken = useUserToken();
  const [danmakuContent, setDanmakuContent] = useState('');
  const [danmakuColor, setDanmakuColor] = useState('#FFFFFF');
  const [showColorPicker, setShowColorPicker] = useState(false);
  const [showInvalidColorAlert, setShowInvalidColorAlert] = useState(false);
  const colorPickerRef = useRef<HTMLDivElement>(null);


  const presetColors = [
    '#FFFFFF', '#FF0000', '#00FF00', '#0000FF', 
    '#FFFF00', '#FF00FF', '#00FFFF', '#FFA500'
  ];


  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (colorPickerRef.current && !colorPickerRef.current.contains(event.target as Node)) {
        setShowColorPicker(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  useEffect(() => {
    if (showInvalidColorAlert) {
      const timer = setTimeout(() => {
        setShowInvalidColorAlert(false);
      }, 2000);
      return () => clearTimeout(timer);
    }
  }, [showInvalidColorAlert]);

  const handleSendDanmaku = async () => {
    if (!isLoggedIn) {
      setShowLoginModal(true);
      return;
    }

    if (!danmakuContent.trim()) return;
    console.log("用户发送了一个弹幕",currentTime);
    // Validate color format
    const colorRegex = /^#([0-9A-F]{3}){1,2}$/i;
    if (!colorRegex.test(danmakuColor)) {
      setShowInvalidColorAlert(true);
      return;
    }

    try {
      await new Promise((resolve, reject) => {
        new PublishDanmakuMessage(
          userToken,
          videoID,
          currentTime,
          danmakuContent,
          danmakuColor
        ).send(
          () => {
            danmakuRef.current?.emit({
                text: danmakuContent,
                time: currentTime,
                style: {
                    font: '20px sans-serif',
                    textAlign: 'start',
                    fillStyle: danmakuColor,
                    strokeStyle: danmakuColor,
                    lineWidth: 1,
            }})
            setDanmakuContent('');
            resolve(true);
          },
          (error) => reject(new Error(`Failed to send danmaku: ${error}`))
        );
      });
    } catch (error) {
      console.error('Error sending danmaku:', error);
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      handleSendDanmaku();
    }
  };

  return (
    <div className="danmaku-input-container">
      <div className="danmaku-input-wrapper">
        <div className="danmaku-text-input">
          
        <div className="color-picker-container" ref={colorPickerRef}>
          <button
            className="color-preview"
            style={{ backgroundColor: danmakuColor }}
            onClick={() => setShowColorPicker(!showColorPicker)}
          />
          {showColorPicker && (
            <div className="color-options">
              {presetColors.map((color) => (
                <button
                  key={color}
                  className="color-option"
                  style={{ backgroundColor: color }}
                  onClick={() => {
                    setDanmakuColor(color);
                    setShowColorPicker(false);
                  }}
                />
              ))}
              <div className="custom-color-input">
                <span>颜色:</span>
                <input
                  type="color"
                  value={danmakuColor}
                  onChange={(e) => setDanmakuColor(e.target.value)}
                />
              </div>
            </div>
          )}
        </div>
          <input
            type="text"
            placeholder="发个弹幕吧~"
            value={danmakuContent}
            onChange={(e) => setDanmakuContent(e.target.value)}
            onKeyPress={handleKeyPress}
            className="danmaku-text-input-word"
          />
          </div>
        <button
          className="send-danmaku-btn"
          onClick={handleSendDanmaku}
        >
          发送
        </button>
      </div>
      {showInvalidColorAlert && (
        <div className="invalid-color-alert">
          颜色格式不正确，请使用十六进制颜色代码(如#FFFFFF)
        </div>
      )}
    </div>
  );
};

export default DanmakuInput;