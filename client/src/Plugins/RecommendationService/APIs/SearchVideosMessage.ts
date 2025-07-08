/**
 * SearchVideosMessage
 * desc: 提供模糊搜索功能，返回匹配的视频列表。
 * @param token: String | null (用户Token，可选)
 * @param searchString: String (搜索关键字，用于匹配视频标题。)
 * @param fetchLimit: Int (返回结果数上限)
 * @return searchResult: Video[] (返回的视频列表，包含匹配的所有视频信息。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';



export class SearchVideosMessage extends TongWenMessage {
    constructor(
        public  token: string | null,
        public  searchString: string,
        public  fetchLimit: number = 20
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["Recommendation"]
    }
}

