local counter_key = KEYS[1]
local owner_key = KEYS[2]
local lease_seconds = tonumber(ARGV[1])

if redis.call('EXISTS', owner_key) == 0 then
    return 0
end

local current = tonumber(redis.call('GET', counter_key) or '0')
if current <= 1 then
    redis.call('DEL', counter_key)
else
    redis.call('DECR', counter_key)
    redis.call('EXPIRE', counter_key, lease_seconds)
end
redis.call('DEL', owner_key)
return 1
