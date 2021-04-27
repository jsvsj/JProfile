import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Data
class CompareInfoVO {
    private String jInfoAName;
    private String jInfoBName;
    private List<GInfo> sameList;
    private Integer allRate;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
class GInfo {

    private String gname;

    private Integer rate;
}

@Data
class JInfo {
    private String jName;
    private List<GInfo> gInfoList;
    private String allRate;
}


public class JProfile {
    public static void main(String[] args) throws IOException {
        List<String> list = FileUtils.readLines(new File(args[0]), "utf-8");
        List<JInfo> jInfoList = new ArrayList<>();
        JInfo jInfo = null;
        for (String s : list) {
            if (StringUtils.isBlank(s)) {
                continue;
            }
            s = s.trim();
            if (jInfo == null) {
                jInfo = new JInfo();
                jInfo.setJName(s);
                continue;
            }
            if (s.contains("前十持仓占比合计")) {
                jInfo.setAllRate(s);
                jInfoList.add(jInfo);
                jInfo = null;
                continue;
            }
            List<GInfo> gInfoList = jInfo.getGInfoList();
            if (gInfoList == null) {
                gInfoList = new ArrayList<>();
            }
            String[] split = s.split("\\s+");
            gInfoList.add(GInfo.builder().gname(split[0]).rate(getRate(split[1])).build());
            jInfo.setGInfoList(gInfoList);
        }
        List<GInfo> collect = jInfoList.stream().flatMap(v -> v.getGInfoList().stream()).collect(Collectors.toList());

        Map<String, Integer> ret = collect.stream().collect(Collectors.groupingBy(GInfo::getGname, Collectors.summingInt(GInfo::getRate)));
        List<Map.Entry<String, Integer>> sortRet = ret.entrySet().stream().sorted((o1, o2) -> o2.getValue() - o1.getValue()).collect(Collectors.toList());
        System.out.println(JSON.toJSONString(sortRet));

        //计算相关性
        List<CompareInfoVO> compareInfoVOList = new ArrayList<>();
        for (int i = 0; i < jInfoList.size(); i++) {
            for (int j = i + 1; j < jInfoList.size(); j++) {
                CompareInfoVO compare = compare(jInfoList.get(i), jInfoList.get(j));
                compareInfoVOList.add(compare);
            }
        }
        compareInfoVOList.sort((o1, o2) -> o2.getAllRate() - o1.getAllRate());


        System.out.println(JSON.toJSONString(compareInfoVOList));
    }

    private static int getRate(String s) {
        try {
            String replace = s.replace("%", "");
            return new BigDecimal(replace).multiply(new BigDecimal(100)).intValueExact();
        } catch (Exception e) {
            System.out.println(s);
            throw e;
        }
    }

    private static CompareInfoVO compare(JInfo a, JInfo b) {
        List<GInfo> gInfoListA = a.getGInfoList();
        List<GInfo> gInfoListB = b.getGInfoList();
        List<GInfo> sameList = new ArrayList<>();
        int allRate = 0;
        for (GInfo gInfoA : gInfoListA) {
            for (GInfo gInfoB : gInfoListB) {
                if (gInfoA.getGname().equals(gInfoB.getGname())) {
                    GInfo build = GInfo.builder().gname(gInfoA.getGname()).rate(gInfoA.getRate() + gInfoB.getRate()).build();
                    sameList.add(build);
                    allRate = allRate + build.getRate();
                }
            }
        }
        CompareInfoVO compareInfoVO = new CompareInfoVO();
        compareInfoVO.setJInfoAName(a.getJName());
        compareInfoVO.setJInfoBName(b.getJName());
        compareInfoVO.setSameList(sameList);
        compareInfoVO.setAllRate(allRate);
        return compareInfoVO;
    }
}
