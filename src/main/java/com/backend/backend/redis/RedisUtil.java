package com.backend.backend.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @Author: goodtimp
 * @Date: 2019/9/15 15:49
 * @description :  redis工具类
 */
@Component
public class RedisUtil {
    private static RedisTemplate<String, Object> redisTemplate;

    /**
     * 需要filter注入那时还不能注入，所以改成静态
     *
     * @param redisTemplate
     */
    @Autowired
    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        RedisUtil.redisTemplate = redisTemplate;
    }


    // ---------------------- 基础操作 ---------------------

    /**
     * 指定key的过期时间(毫秒)
     *
     * @param key
     * @param time 时间（ms>0）
     * @return
     */
    public static Boolean expire(String key, Long time) {
        try {
            if (time > 0) {
                
                redisTemplate.expire(key, time, TimeUnit.MILLISECONDS);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取key的失效时间(毫秒)
     *
     * @param key
     * @return 毫秒 0 永久有效
     */
    public static Long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
    }

    /**
     * 删除 ，可以多条
     *
     * @param key "1","2","3" ["1","2","3"]都可以
     */
    public static void del(String... key) {
        if (key != null && key.length > 0) {
            redisTemplate.delete(CollectionUtils.arrayToList(key));
        }
    }

    /**
     * 得到前缀为传入值所有的key
     *
     * @param prefix 要求含有的前缀，不传清除所有
     * @return
     */
    public static Set getAllKey(String... prefix) {
        Set<String> keys = new HashSet<>();
        if (prefix == null || prefix.length == 0) {
            keys.containsAll(redisTemplate.keys("*"));
        } else {
            for (String item : prefix) {
                Set<String> keyRedis = redisTemplate.keys(item + "*");
                keys.addAll(keyRedis);
            }
        }
        return keys;
    }

    /**
     * 得到前缀为传入值 redis所有中key的数量
     *
     * @param prefix 要求含有的前缀
     * @return
     */
    public static Integer getAllKeyLength(String... prefix) {
        return getAllKey(prefix).size();
    }

    /**
     * 清除前缀为传入值的所有缓存
     *
     * @param prefix
     * @return
     */
    public static Integer clear(String... prefix) {
        Set<String> keys = getAllKey(prefix);
        for (String item : keys) {
            redisTemplate.delete(item);
        }
        return keys.size();
    }

    // ---------------------- String（普通键值对） ---------------------

    /**
     * 根据key得到value
     *
     * @param key
     * @return 值
     */
    public static Object get(String key) {
        try {
            return key == null ? null : redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 修改
     *
     * @param key
     * @param value
     * @return true 成功  false 失败
     */
    public static Boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 修改有时间限制的数据，但是不修改他的过期时间
     *
     * @param key
     * @param value
     * @return
     */
    public static Boolean setAndNotExpire(String key, Object value) {
        try {
            System.out.println("------------------------------------------------------");
            System.out.println(getExpire(key));
            System.out.println("------------------------------------------------------");
            if (getExpire(key).equals(-2L)) return false;
            set(key, value, getExpire(key));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 普通set 设置时间
     *
     * @param key
     * @param value
     * @param time  time（毫秒）要大于0 如果time小于等于0 将设置无限期
     * @return true 成功  false 失败
     */
    public static Boolean set(String key, Object value, Long time) {
        try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, TimeUnit.MILLISECONDS);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 递增
     *
     * @param key
     * @param delta（递增数量 大于0）
     * @return
     */
    public static Long incr(String key, Long delta) {
        if (delta < 0) {
            throw new RuntimeException("递增因子必须大于0");
        }
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 递减
     *
     * @param key
     * @param delta（递减数量 大于0）
     * @return
     */
    public static Long decr(String key, Long delta) {
        if (delta < 0) {
            throw new RuntimeException("递减因子必须大于0");
        }
        return redisTemplate.opsForValue().decrement(key, -delta);
    }

    // ---------------------- hash map ---------------------

    /**
     * 获取hash内某个子键的值 get方法
     *
     * @param key  键
     * @param item 子健
     * @return 值
     */
    public static Object hget(String key, String item) {
        return redisTemplate.opsForHash().get(key, item);
    }

    /**
     * 获取一个hash的所有键值对
     *
     * @param key
     * @return
     */
    public static Map<Object, Object> hmget(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * HashSet
     *
     * @param key 键
     * @param map 对应多个键值
     * @return true 成功 false 失败
     */
    public static Boolean hmset(String key, Map<String, Object> map) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * HashSet 并设置时间
     *
     * @param key  键
     * @param map  对应多个键值
     * @param time 时间(毫秒)
     * @return true成功 false失败
     */
    public static Boolean hmset(String key, Map<String, Object> map, Long time) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 向一张hash表中放入数据,如果不存在将创建
     *
     * @param key   键
     * @param item  项
     * @param value 值
     * @return true 成功 false失败
     */
    public static Boolean hset(String key, String item, Object value) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 向一张hash表中放入数据,如果不存在将创建
     *
     * @param key   键
     * @param item  项
     * @param value 值
     * @param time  时间(毫秒)  注意:如果已存在的hash表有时间,这里将会替换原有的时间
     * @return true 成功 false失败
     */
    public static Boolean hset(String key, String item, Object value, Long time) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除hash表中的值
     *
     * @param key  键 不能为null
     * @param item 项 可以使多个 不能为null
     */
    public static void hdel(String key, Object... item) {
        redisTemplate.opsForHash().delete(key, item);
    }

    /**
     * 判断hash表中是否有该项的值
     *
     * @param key  键 不能为null
     * @param item 项 不能为null
     * @return true 存在 false不存在
     */
    public static Boolean hHasKey(String key, String item) {
        return redisTemplate.opsForHash().hasKey(key, item);
    }

    /**
     * hash递增 如果不存在,就会创建一个 并把新增后的值返回
     *
     * @param key  键
     * @param item 项
     * @param by   要增加几(大于0)
     * @return
     */
    public static Double hincr(String key, String item, Double by) {
        return redisTemplate.opsForHash().increment(key, item, by);
    }

    /**
     * hash递增 如果不存在,就会创建一个 并把新增后的值返回
     *
     * @param key  键
     * @param item 项
     * @param by   要增加几(大于0)
     * @return
     */
    public static Long hincr(String key, String item, Long by) {
        return redisTemplate.opsForHash().increment(key, item, by);
    }

    /**
     * hash递减
     *
     * @param key  键
     * @param item 项
     * @param by   要减少记(小于0)
     * @return
     */
    public static Double hdecr(String key, String item, Double by) {
        return redisTemplate.opsForHash().increment(key, item, -by);
    }

    /**
     * hash递减
     *
     * @param key  键
     * @param item 项
     * @param by   要减少记(小于0)
     * @return
     */
    public static Long hdecr(String key, String item, Long by) {
        return redisTemplate.opsForHash().increment(key, item, -by);
    }

    //============================set=============================

    /**
     * 根据key获取Set中的所有值
     *
     * @param key 键
     * @return
     */
    public static Set<Object> sGet(String key) {
        try {
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 根据value从一个set中查询,是否存在
     *
     * @param key   键
     * @param value 值
     * @return true 存在 false不存在
     */
    public static Boolean sHasKey(String key, Object value) {
        try {
            return redisTemplate.opsForSet().isMember(key, value);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将数据放入set缓存
     *
     * @param key    键
     * @param values 值 可以是多个
     * @return 成功个数
     */
    public static Long sSet(String key, Object... values) {
        try {
            return redisTemplate.opsForSet().add(key, values);
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        }
    }

    /**
     * 将set数据放入缓存
     *
     * @param key    键
     * @param time   时间(秒)
     * @param values 值 可以是多个
     * @return 成功个数
     */
    public static Long sSetAndTime(String key, Long time, Object... values) {
        try {
            Long count = redisTemplate.opsForSet().add(key, values);
            if (time > 0) {
                expire(key, time);
            }
            return count;
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        }
    }

    /**
     * 获取set缓存的长度
     *
     * @param key 键
     * @return
     */
    public static Long sGetSetSize(String key) {
        try {
            return redisTemplate.opsForSet().size(key);
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        }
    }

    /**
     * 移除值为value的
     *
     * @param key    键
     * @param values 值 可以是多个
     * @return 移除的个数
     */
    public static Long setRemove(String key, Object... values) {
        try {
            Long count = redisTemplate.opsForSet().remove(key, values);
            return count;
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        }
    }

    /**
     * 取key与other key的差集
     *
     * @param key       不能为null
     * @param otherKeys
     * @return 集合, 如果不传入otherKeys则返回key对应的值
     */
    public static Set<Object> difference(String key, String... otherKeys) {
        if (otherKeys != null && otherKeys.length > 0) {
            if (otherKeys.length == 1) {
                return redisTemplate.opsForSet().difference(key, otherKeys[0]);
            } else {
                return redisTemplate.opsForSet().difference(key, Arrays.asList(otherKeys));
            }
        }
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * 取key与other key的交集
     *
     * @param key       不能为null
     * @param otherKeys 不能为null
     * @return 集合
     */
    public static Set<Object> intersect(String key, String... otherKeys) {
        if (otherKeys != null && otherKeys.length > 0) {
            if (otherKeys.length == 1) {
                return redisTemplate.opsForSet().intersect(key, otherKeys[0]);
            } else {
                return redisTemplate.opsForSet().intersect(key, Arrays.asList(otherKeys));
            }
        }
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * 取key与other key的并集
     *
     * @param key       不能为null
     * @param otherKeys 不能为null
     * @return 集合
     */
    public static Set<Object> union(String key, String... otherKeys) {
        if (otherKeys != null && otherKeys.length > 0) {
            if (otherKeys.length == 1) {
                return redisTemplate.opsForSet().union(key, otherKeys[0]);
            } else {
                return redisTemplate.opsForSet().union(key, Arrays.asList(otherKeys));
            }
        }
        return redisTemplate.opsForSet().members(key);
    }

    //===============================list=================================

    /**
     * 获取list缓存的内容
     *
     * @param key   键
     * @param start 开始
     * @param end   结束  0 到 -1代表所有值
     * @return
     */
    public static List<Object> lGet(String key, Long start, Long end) {
        try {
            return redisTemplate.opsForList().range(key, start, end);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取list缓存的长度
     *
     * @param key 键
     * @return
     */
    public static Long lGetListSize(String key) {
        try {
            return redisTemplate.opsForList().size(key);
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        }
    }

    /**
     * 通过索引 获取list中的值
     *
     * @param key   键
     * @param index 索引  index>=0时， 0 表头，1 第二个元素，依次类推；index<0时，-1，表尾，-2倒数第二个元素，依次类推
     * @return
     */
    public static Object lGetIndex(String key, Long index) {
        try {
            return redisTemplate.opsForList().index(key, index);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key   键
     * @param value 值
     * @return
     */
    public static Boolean lSet(String key, Object value) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key   键
     * @param value 值
     * @param time  时间(秒)
     * @return
     */
    public static Boolean lSet(String key, Object value, Long time) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key   键
     * @param value 值
     * @return
     */
    public static Boolean lSet(String key, List<Object> value) {
        try {
            redisTemplate.opsForList().rightPushAll(key, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key   键
     * @param value 值
     * @param time  时间(秒)
     * @return
     */
    public static Boolean lSet(String key, List<Object> value, Long time) {
        try {
            redisTemplate.opsForList().rightPushAll(key, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 根据索引修改list中的某条数据
     *
     * @param key   键
     * @param index 索引
     * @param value 值
     * @return
     */
    public static Boolean lUpdateIndex(String key, Long index, Object value) {
        try {
            redisTemplate.opsForList().set(key, index, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 移除N个值为value
     *
     * @param key   键
     * @param count 移除多少个
     * @param value 值
     * @return 移除的个数
     */
    public static Long lRemove(String key, Long count, Object value) {
        try {
            Long remove = redisTemplate.opsForList().remove(key, count, value);
            return remove;
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        }
    }
}
