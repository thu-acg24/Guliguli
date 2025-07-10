import React, { useState, useRef } from "react";
import { useNavigateMember } from "Globals/Navigate";
import { useUserToken } from "Globals/GlobalStore";
import { UploadVideoMessage } from "Plugins/VideoService/APIs/UploadVideoMessage";
import { materialAlertError, materialAlertSuccess } from "Plugins/CommonUtils/Gadgets/AlertGadget";
import { memberPagePath } from "./MemberPage";
import { VideoBasicInfo, VideoUpload, CoverUpload } from "./VideoEdit";
import { set } from "lodash";

const MemberUpload: React.FC = () => {
    const { navigateMember } = useNavigateMember();
    const userToken = useUserToken();

    const [step, setStep] = useState(1); // 1: 基本信息, 2: 上传视频, 3: 设置封面
    const [videoID, setVideoID] = useState<number | null>(null);
    const [refreshKey, setRefreshKey] = useState(0); // 用于强制刷新封面组件
    const [modalConfig, setModalConfig] = useState<{
        show: boolean;
        title: string;
        message: string;
        buttonText: string;
        onConfirm: () => void;
    }>({
        show: false,
        title: '',
        message: '',
        buttonText: '',
        onConfirm: () => { }
    });

    const handleCreateVideo = async (title: string, description: string, tags: string[]) => {
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

        // 显示成功弹窗
        setModalConfig({
            show: true,
            title: '视频基本信息创建成功！',
            message: '您的视频信息已成功保存，现在可以上传视频文件了。',
            buttonText: '上传视频',
            onConfirm: () => {
                setModalConfig(prev => ({ ...prev, show: false }));
                setStep(2);
            }
        });
        console.log("视频创建成功", "继续上传视频文件");
    };

    const handleVideoUploaded = () => {
        setRefreshKey(prev => prev + 1); 
        setModalConfig({
            show: true,
            title: '视频上传成功！',
            message: '您的视频文件已成功上传，现在可以设置视频封面了。',
            buttonText: '设置封面',
            onConfirm: () => {
                setModalConfig(prev => ({ ...prev, show: false }));
                setStep(3);
            }
        });
    };

    const handleFinish = () => {
        setModalConfig({
            show: true,
            title: '视频上传完成！',
            message: '您的视频已成功上传，等待审核！感谢您的投稿。',
            buttonText: '返回创作中心',
            onConfirm: () => {
                setModalConfig(prev => ({ ...prev, show: false }));
                navigateMember();
            }
        });
    };

    const renderStep1 = () => (
        <div className="member-upload-step-content">
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

    const renderSuccessModal = () => (
        <div className="member-upload-modal-overlay">
            <div className="member-upload-modal">
                <div className="member-upload-modal-content">
                    <div className="member-upload-success-icon">
                        <div className="success-checkmark">
                            <div className="check-icon">
                                <span className="icon-line line-tip"></span>
                                <span className="icon-line line-long"></span>
                                <div className="icon-circle"></div>
                                <div className="icon-fix"></div>
                            </div>
                        </div>
                    </div>
                    <h2 className="member-upload-success-title">{modalConfig.title}</h2>
                    <p className="member-upload-success-message">
                        {modalConfig.message}
                    </p>
                    <div className="member-upload-modal-actions">
                        <button
                            className="member-upload-continue-btn"
                            onClick={modalConfig.onConfirm}
                        >
                            {modalConfig.buttonText}
                        </button>
                    </div>
                </div>
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

            {modalConfig.show && renderSuccessModal()}
        </div>
    );
};

export default MemberUpload;
