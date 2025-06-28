/**
 * SearchVideosCountMessage
 * desc: 查询符合标题条件的视频总数。
 * @param searchString: String (搜索关键字，用于模糊匹配视频标题。)
 * @return searchResultCount: Int (符合条件的视频总数。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class SearchVideosCountMessage extends TongWenMessage {
    constructor(
        public  searchString: string
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10011"
    }
}

