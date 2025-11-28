package com.mind_mate.home.component;
import com.mind_mate.home.entity.EmojiList;
import com.mind_mate.home.repository.EmojiListRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class EmojiDataLoader implements CommandLineRunner {

    private final EmojiListRepository emojiRListRepository;

    // 생성자 주입
    public EmojiDataLoader(EmojiListRepository emojiRepository) {
        this.emojiRListRepository = emojiRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // 🚨 데이터가 이미 존재하는지 확인 (중복 삽입 방지)
        if (emojiRListRepository.count() == 0) { 
            
            // 삽입할 이모지 리스트 정의
            List<EmojiList> initialEmojis = Arrays.asList(
                createEmoji(1L, "heart", "/emojis/heart.png"),
                createEmoji(2L, "love", "/emojis/love.png"),
                createEmoji(3L, "happy", "/emojis/happy.png"),
                createEmoji(4L, "relax", "/emojis/relax.png"),
                createEmoji(5L, "smile", "/emojis/smile.png"),
                createEmoji(6L, "wow", "/emojis/wow.png"),
                createEmoji(7L, "joy", "/emojis/joy.png"),
                createEmoji(8L, "meh", "/emojis/meh.png"),
                createEmoji(9L, "unsure", "/emojis/unsure.png"),
                createEmoji(10L, "sad", "/emojis/sad.png"),
                createEmoji(11L, "spin", "/emojis/spin.png"),
                createEmoji(12L, "tears", "/emojis/tears.png"),
                createEmoji(13L, "shock", "/emojis/shock.png"),
                createEmoji(14L, "unwell", "/emojis/unwell.png"),
                createEmoji(15L, "angry", "/emojis/angry.png"),
                createEmoji(16L, "unknown", "/emojis/unknown.png")
            );

            // 리스트의 모든 이모지를 한 번에 저장
            emojiRListRepository.saveAll(initialEmojis);
            
            System.out.println("✅ 초기 이모지 데이터 15개 삽입 완료!");
        }
    }
    
    // 이모지 객체를 생성하는 헬퍼 메서드
    private EmojiList createEmoji(Long id, String type, String imageUrl) {
    	EmojiList emojilist = new EmojiList();
        // ID는 DB의 Auto-Increment를 사용하는 것이 일반적이므로, 
        // 만약 @GeneratedValue를 사용한다면 이 줄(setId)은 제거해야 합니다.
        
//    	emojilist.setId(id); 
    	emojilist.setType(type);
        // name 필드가 엔티티에 있다면 여기에 적절한 이름도 추가해야 합니다.
        // emoji.setName(type.toUpperCase()); 
    	emojilist.setImageUrl(imageUrl);
        return emojilist;
    }
}