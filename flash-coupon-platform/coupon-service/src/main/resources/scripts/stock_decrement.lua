-- 쿠폰 재고 원자적 감소 스크립트
-- KEYS[1]: 재고 키 (coupon:stock:{id})
-- KEYS[2]: 발급 사용자 Set 키 (coupon:issued:{id})
-- ARGV[1]: 사용자 ID
-- 반환값: 1 = 성공, 0 = 재고 없음, -1 = 이미 발급됨

local stockKey = KEYS[1]
local issuedKey = KEYS[2]
local userId = ARGV[1]

-- 이미 발급받은 사용자인지 확인
if redis.call('SISMEMBER', issuedKey, userId) == 1 then
    return -1
end

-- 현재 재고 확인
local stock = tonumber(redis.call('GET', stockKey) or 0)

if stock <= 0 then
    return 0
end

-- 재고 감소 및 사용자 등록 (원자적 실행)
redis.call('DECR', stockKey)
redis.call('SADD', issuedKey, userId)

return 1
