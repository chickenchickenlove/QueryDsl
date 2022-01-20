package study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MemberDto {

    private String username;
    private int age;


    /**
     * 어노테이션 QueryProjection 달아주고 gradle 가서 compilequerydsl 해준다.
     * 그러면 DTO도 Q파일로 만들어진다.
     *
     */

    @QueryProjection
    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }



}
