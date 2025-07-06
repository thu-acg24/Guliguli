import React, { useState, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { useUserToken } from "Globals/GlobalStore";
import { UploadVideoMessage } from "Plugins/VideoService/APIs/UploadVideoMessage";
import { materialAlertError, materialAlertSuccess } from "Plugins/CommonUtils/Gadgets/AlertGadget";
import { memberPagePath } from "./MemberPage";
import { VideoBasicInfo, VideoUpload, CoverUpload } from "./VideoEdit";

const MemberUpload: React.FC = () => {
    const navigate = useNavigate();
    const userToken = useUserToken();

    const [step, setStep] = useState(1); // 1: 基本信息, 2: 上传视频, 3: 设置封面
    const [videoID, setVideoID] = useState<number | null>(null);

    const handleCreateVideo = async (title: string, description: string, tags: string[]) => {
        if (!userToken) {
            throw new Error("未登录");
        }

        const response = await new Promise<string>((resolve, reject) => {
            new UploadVideoMessage(
                userToken,
                title,
                description,
                tags
            ).send(
                (info: string) => resolve(info),
                (error: string) => reject(new Error(error))
            );
        });

        const createdVideoID = parseInt(response);
        setVideoID(createdVideoID);
        setStep(2);
        materialAlertSuccess("视频创建成功", "请继续上传视频文件");
    };

    const handleVideoUploaded = () => {
        setStep(3);
    };

    const handleFinish = () => {
        materialAlertSuccess("视频上传完成", "您的视频已成功上传，等待审核");
        navigate(memberPagePath);
    };

    const renderStep1 = () => (
        <div className="member-upload-step-content">
            <h2>基本信息</h2>
            <VideoBasicInfo
                isCreating={true}
                onSubmit={handleCreateVideo}
            />
        </div>
    );

    const renderStep2 = () => (
        <div className="member-upload-step-content">
            <VideoUpload
                isCreating={true}
                videoID={videoID!}
                onVideoUploaded={handleVideoUploaded}
            />
        </div>
    );

    const renderStep3 = () => (
        <div className="member-upload-step-content">
            <h2>设置封面</h2>
            <CoverUpload
                videoID={videoID!}
            />
            <div className="member-cover-upload" style={{ marginTop: '20px' }}>
                <button
                    className="member-cover-upload-btn"
                    onClick={handleFinish}
                >
                    完成上传
                </button>
            </div>
        </div>
    );

    return (
        <div className="member-upload-container">
            <h1 className="member-upload-title">上传视频</h1>

            <div className="member-upload-steps">
                <div className={`member-upload-step ${step >= 1 ? 'active' : ''} ${step > 1 ? 'completed' : ''}`}>
                    <div className="member-upload-step-number">1</div>
                    <div className="member-upload-step-text">基本信息</div>
                </div>
                <div className={`member-upload-step ${step >= 2 ? 'active' : ''} ${step > 2 ? 'completed' : ''}`}>
                    <div className="member-upload-step-number">2</div>
                    <div className="member-upload-step-text">上传视频</div>
                </div>
                <div className={`member-upload-step ${step >= 3 ? 'active' : ''} ${step > 3 ? 'completed' : ''}`}>
                    <div className="member-upload-step-number">3</div>
                    <div className="member-upload-step-text">设置封面</div>
                </div>
            </div>

            {step === 1 && renderStep1()}
            {step === 2 && renderStep2()}
            {step === 3 && renderStep3()}
        </div>
    );
};

export default MemberUpload;
