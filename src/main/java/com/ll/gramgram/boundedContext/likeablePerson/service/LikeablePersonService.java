package com.ll.gramgram.boundedContext.likeablePerson.service;

import com.ll.gramgram.base.rsData.RsData;
import com.ll.gramgram.boundedContext.instaMember.entity.InstaMember;
import com.ll.gramgram.boundedContext.instaMember.service.InstaMemberService;
import com.ll.gramgram.boundedContext.likeablePerson.entity.LikeablePerson;
import com.ll.gramgram.boundedContext.likeablePerson.repository.LikeablePersonRepository;
import com.ll.gramgram.boundedContext.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeablePersonService {
    private final LikeablePersonRepository likeablePersonRepository;
    private final InstaMemberService instaMemberService;

    @Transactional
    public RsData<LikeablePerson> like(Member member, String username, int attractiveTypeCode) {
        if ( member.hasConnectedInstaMember() == false ) {
            return RsData.of("F-2", "먼저 본인의 인스타그램 아이디를 입력해야 합니다.");
        }

        if (member.getInstaMember().getUsername().equals(username)) {
            return RsData.of("F-1", "본인을 호감상대로 등록할 수 없습니다.");
        }

        //호감 상대 등록 중 예외에 대한 처리
        List<LikeablePerson> toInstaMemberExist = likeablePersonRepository.findByFromInstaMemberIdAndToInstaMemberUsername(member.getInstaMember().getId(), username);

        if(!toInstaMemberExist.isEmpty()){
            for(LikeablePerson l : toInstaMemberExist){
                if(l.getAttractiveTypeCode() == attractiveTypeCode){
                    return RsData.of("F-4", "동일한 상대를 등록할 수 없습니다.");
                }
            }
            toInstaMemberExist.get(0).setAttractiveTypeCode(attractiveTypeCode);
            return RsData.of("S-2", "입력하신 인스타유저(%s)의 유형을 변경하였습니다.".formatted(username));
        }
        else if (likeablePersonRepository.countByFromInstaMemberId(member.getInstaMember().getId()) > 10) {
            return RsData.of("F-3", "등록한 호감 상대가 10명을 초과했습니다.");
        }

        InstaMember toInstaMember = instaMemberService.findByUsernameOrCreate(username).getData();

        LikeablePerson likeablePerson = LikeablePerson
                .builder()
                .fromInstaMember(member.getInstaMember()) // 호감을 표시하는 사람의 인스타 멤버
                .fromInstaMemberUsername(member.getInstaMember().getUsername()) // 중요하지 않음
                .toInstaMember(toInstaMember) // 호감을 받는 사람의 인스타 멤버
                .toInstaMemberUsername(toInstaMember.getUsername()) // 중요하지 않음
                .attractiveTypeCode(attractiveTypeCode) // 1=외모, 2=능력, 3=성격
                .build();

        likeablePersonRepository.save(likeablePerson); // 저장

        return RsData.of("S-1", "입력하신 인스타유저(%s)를 호감상대로 등록되었습니다.".formatted(username), likeablePerson);
    }

    public List<LikeablePerson> findByFromInstaMemberId(Long fromInstaMemberId) {
        return likeablePersonRepository.findByFromInstaMemberId(fromInstaMemberId);
    }

    @Transactional
    public RsData<LikeablePerson> unlike(Member member, long id) {
        boolean check = false;
        if ( member.hasConnectedInstaMember() == false ) {
            return RsData.of("F-2", "먼저 본인의 인스타그램 아이디를 입력해야 합니다.");
        }

        if(likeablePersonRepository.findById(id).get().getFromInstaMember().getId().equals(member.getInstaMember().getId())){
            //삭제하려는 호감표시의 사용자 ID와 현재 접속한 유저의 인스타 id가 일치할 경우에만 삭제
            likeablePersonRepository.deleteById(id);
            return RsData.of("S-1", "입력하신 인스타유저를 호감상대에서 삭제하였습니다.");
        }

        return RsData.of("F-3", "다른 유저의 호감상대를 삭제할 수 없습니다.");
    }


}
