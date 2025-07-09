import React, { useState } from "react";
import { Modal, Button, Checkbox, Input, message } from "antd";
import { ReportVideoContentMessage } from "Plugins/ReportService/APIs/ReportVideoContentMessage";
import { ReportDanmakuContentMessage } from "Plugins/ReportService/APIs/ReportDanmakuContentMessage";
import { ReportCommentContentMessage } from "Plugins/ReportService/APIs/ReportCommentContentMessage";
import { useUserToken } from "Globals/GlobalStore";
import { materialAlertError,materialAlertSuccess } from "Plugins/CommonUtils/Gadgets/AlertGadget";
import "./ReportComponents.css"; // 导入CSS文件
const { TextArea } = Input;

interface CommonReportProps {
  visible: boolean;
  onCancel: () => void;
  onSuccess?: () => void;
}

interface VideoReportProps extends CommonReportProps {
  videoID: number;
}

interface DanmakuReportProps extends CommonReportProps {
  danmakuID: number;
}

interface CommentReportProps extends CommonReportProps {
  commentID: number;
}

const commonReasons = [
  "包含不适当内容",
  "包含辱骂或人身攻击",
  "包含垃圾广告",
  "包含色情或低俗内容",
  "包含暴力或恐怖内容",
  "其他"
];
const renderModalContent = (
  selectedReasons: string[],
  setSelectedReasons: (reasons: string[]) => void,
  otherReason: string,
  setOtherReason: (reason: string) => void,
  title: string
) => {
  return (
    <>
      <div className="reportcomponents-modal-title">{title}</div>
      <div className="reportcomponents-reasons-container">
        <div className="reportcomponents-reasons-title">请选择举报理由：</div>
        <Checkbox.Group
          className="reportcomponents-checkbox-group"
          options={commonReasons.map(reason => ({
            label: reason,
            value: reason,
            className: "reportcomponents-checkbox-item"
          }))}
          onChange={(checkedValues) => setSelectedReasons(checkedValues as string[])}
        />
      </div>
      {selectedReasons.includes("其他") && (
        <div className="reportcomponents-other-reason">
          <TextArea
            className="reportcomponents-textarea"
            rows={4}
            placeholder="请填写具体举报原因"
            value={otherReason}
            onChange={(e) => setOtherReason(e.target.value)}
          />
        </div>
      )}
    </>
  );
};

const renderModalFooter = (
  onCancel: () => void,
  handleSubmit: () => void,
  submitting: boolean
) => {
  return (
    <div className="reportcomponents-footer">
      <Button key="back" onClick={onCancel}>
        取消
      </Button>
      <Button 
        key="submit" 
        type="primary" 
        className="reportcomponents-submit-btn"
        loading={submitting}
        onClick={handleSubmit}
      >
        提交
      </Button>
    </div>
  );
};

export const VideoReportModal: React.FC<VideoReportProps> = ({
  visible,
  onCancel,
  onSuccess,
  videoID
}) => {
  const userToken = useUserToken();
  const [selectedReasons, setSelectedReasons] = useState<string[]>([]);
  const [otherReason, setOtherReason] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async () => {
    if (selectedReasons.length === 0) {
      materialAlertError("请至少选择一项举报理由","注意")
      return;
    }

    if (selectedReasons.includes("其他") && !otherReason.trim()) {
      materialAlertError("请填写具体举报原因","注意")
      return;
    }

    setSubmitting(true);
    try {
      const reason = selectedReasons.includes("其他") 
        ? `${selectedReasons.filter(r => r !== "其他").join("、")}、${otherReason}`
        : selectedReasons.join("、");

      await new Promise((resolve, reject) => {
        new ReportVideoContentMessage(userToken, videoID, reason).send(
          () => resolve(true),
          (error) => reject(new Error(error))
        );
      });

      materialAlertSuccess("举报提交成功，感谢您的反馈！")
      onSuccess?.();
      onCancel();
    } catch (error) {
      console.error("举报失败:", error);
      materialAlertError("举报提交失败，请稍后再试","注意")
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      title={null}
      visible={visible}
      onCancel={onCancel}
      footer={null}
      width={600}
    >
      {renderModalContent(
        selectedReasons,
        setSelectedReasons,
        otherReason,
        setOtherReason,
        "视频举报"
      )}
      {renderModalFooter(onCancel, handleSubmit, submitting)}
    </Modal>
  );
};


export const DanmakuReportModal: React.FC<DanmakuReportProps> = ({
  visible,
  onCancel,
  onSuccess,
  danmakuID
}) => {
  const userToken = useUserToken();
  const [selectedReasons, setSelectedReasons] = useState<string[]>([]);
  const [otherReason, setOtherReason] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async () => {
    if (selectedReasons.length === 0) {
      materialAlertError("请至少选择一项举报理由","注意")
      return;
    }

    if (selectedReasons.includes("其他") && !otherReason.trim()) {
      materialAlertError("请填写具体举报原因","注意")
      return;
    }

    setSubmitting(true);
    try {
      const reason = selectedReasons.includes("其他") 
        ? `${selectedReasons.filter(r => r !== "其他").join("、")}、${otherReason}`
        : selectedReasons.join("、");

      await new Promise((resolve, reject) => {
        new ReportDanmakuContentMessage(userToken, danmakuID, reason).send(
          () => resolve(true),
          (error) => reject(new Error(error))
        )});

      materialAlertSuccess("举报提交成功，感谢您的反馈！")
      onSuccess?.();
      onCancel();
    } catch (error) {
      console.error("举报失败:", error);
      materialAlertError("举报提交失败，请稍后再试","注意")
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      title={null}
      visible={visible}
      onCancel={onCancel}
      footer={null}
      width={600}
    >
      {renderModalContent(
        selectedReasons,
        setSelectedReasons,
        otherReason,
        setOtherReason,
        "弹幕举报"
      )}
      {renderModalFooter(onCancel, handleSubmit, submitting)}
    </Modal>
  );
};

export const CommentReportModal: React.FC<CommentReportProps> = ({
  visible,
  onCancel,
  onSuccess,
  commentID
}) => {
  const userToken = useUserToken();
  const [selectedReasons, setSelectedReasons] = useState<string[]>([]);
  const [otherReason, setOtherReason] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async () => {
    if (selectedReasons.length === 0) {
      materialAlertError("请至少选择一项举报理由","注意")
      return;
    }

    if (selectedReasons.includes("其他") && !otherReason.trim()) {
      materialAlertError("请填写具体举报原因","注意")
      return;
    }

    setSubmitting(true);
    try {
      const reason = selectedReasons.includes("其他") 
        ? `${selectedReasons.filter(r => r !== "其他").join("、")}、${otherReason}`
        : selectedReasons.join("、");

      await new Promise((resolve, reject) => {
        new ReportCommentContentMessage(userToken, commentID, reason).send(
          () => resolve(true),
          (error) => reject(new Error(error))
        )});

      materialAlertSuccess("举报提交成功，感谢您的反馈！")
      onSuccess?.();
      onCancel();
    } catch (error) {
      console.error("举报失败:", error);
      materialAlertError("举报提交失败，请稍后再试","注意")
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      title={null}
      visible={visible}
      onCancel={onCancel}
      footer={null}
      width={600}
    >
      {renderModalContent(
        selectedReasons,
        setSelectedReasons,
        otherReason,
        setOtherReason,
        "评论举报"
      )}
      {renderModalFooter(onCancel, handleSubmit, submitting)}
    </Modal>
  );
};
