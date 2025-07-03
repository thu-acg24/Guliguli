/**
 * ChangeVideoStatusMessage
 * desc: 修改视频审核状态
 * @param token: String (用户身份凭证，用于校验权限)
 * @param videoID: Int (视频ID，用于定位要修改状态的视频)
 * @param status: VideoStatus (目标审核状态，例如待审核、审核通过或审核拒绝)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';
import { VideoStatus } from 'Plugins/VideoService/Objects/VideoStatus';


export class ChangeVideoStatusMessage extends TongWenMessage {
    constructor(
        public  token: string,
        public  videoID: number,
        public  status: VideoStatus
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["Video"]
    }
}

