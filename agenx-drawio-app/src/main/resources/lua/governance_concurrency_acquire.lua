local counter_key = KEYS[1]
local owner_key = KEYS[2]
local limit = tonumber(ARGV[1])
local lease_seconds = tonumber(ARGV[2])
local owner_value = ARGV[3]

if redis.call('EXISTS', owner_key) == 1 then
    redis.call('EXPIRE', owner_key, lease_seconds)
    redis.call('EXPIRE', counter_key, lease_seconds)
    return 1
end

local current = tonumber(redis.call('GET', counter_key) or '0')
if current >= limit then
    return 0
end

redis.call('INCR', counter_key)
redis.call('SET', owner_key, owner_value, 'EX', lease_seconds)
redis.call('EXPIRE', counter_key, lease_seconds)
return 1
