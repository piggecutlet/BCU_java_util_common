package common.system.files;

import common.CommonStatic;
import common.pack.Context.ErrType;
import common.system.fake.FakeImage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * 仮想ファイルの内容を、バイト列・画像・ストリームとして提供するデータ源。
 * 呼び出し側がストリームを閉じるため、各呼び出しでは独立して読み取れるストリームを返す必要がある。
 */
public interface FileData {

	default byte[] getBytes() {
		try (InputStream is = getStream()) {
			byte[] ans = new byte[size()];
			int r = is.read(ans);
			if (r != size())
				CommonStatic.ctx.printErr(ErrType.FATAL, "failed to read data");
			return ans;
		} catch (Exception e) {
			CommonStatic.ctx.noticeErr(e, ErrType.FATAL, "failed to read data");
			return null;
		}
	}

	FakeImage getImg();

	/**
	 * 先頭から読み取れる新しいストリームを返す。
	 */
	InputStream getStream();

	default Queue<String> readLine() {
        try (InputStream is = getStream()) {
            try {
                Queue<String> ans = new ArrayDeque<>();
                InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                BufferedReader reader = new BufferedReader(isr);
                String temp;
                while ((temp = reader.readLine()) != null)
                    ans.add(temp);
                reader.close();
                isr.close();
                return ans;
            } catch (Exception e) {
                CommonStatic.ctx.noticeErr(e, ErrType.FATAL, "failed to read lines");
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
			return null;
        }
	}

	int size();

}
