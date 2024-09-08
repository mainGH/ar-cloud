/*
 Navicat Premium Data Transfer

 Source Server         : Ar_Test
 Source Server Type    : MySQL
 Source Server Version : 80035
 Source Host           : 8.222.160.178:3306
 Source Schema         : sys_manager

 Target Server Type    : MySQL
 Target Server Version : 80035
 File Encoding         : 65001

 Date: 30/11/2023 13:59:51
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for bi_withdraw_order_month
-- ----------------------------
DROP TABLE IF EXISTS `bi_withdraw_order_month`;
CREATE TABLE `bi_withdraw_order_month`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `date_time` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '日期',
  `merchant_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '商户编码',
  `money` decimal(20, 2) NULL DEFAULT NULL COMMENT '订单金额',
  `actual_money` decimal(20, 2) NULL DEFAULT NULL COMMENT '实际金额',
  `order_num` bigint NULL DEFAULT NULL COMMENT '下单总笔数',
  `success_order_num` bigint NULL DEFAULT NULL COMMENT '成功笔数',
  `total_fee` decimal(20, 2) NULL DEFAULT NULL COMMENT '总费用',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `last_minute` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '上一次执行时间：22:05',
  `over_time_num` bigint NULL DEFAULT NULL COMMENT '匹配超时订单笔数',
  `cancel_match_num` bigint NULL DEFAULT NULL COMMENT '取消匹配订单数量',
  `appeal_num` bigint NULL DEFAULT NULL COMMENT '申诉订单数量',
  `continue_match_num` bigint NULL DEFAULT NULL COMMENT '继续匹配订单数量',
  `match_duration` bigint NULL DEFAULT NULL COMMENT '匹配总时长',
  `finish_duration` bigint NULL DEFAULT NULL COMMENT '完成总时长',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `dm`(`date_time` ASC, `merchant_code` ASC) USING BTREE COMMENT '时间唯一索引'
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '代付订单月报' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of bi_withdraw_order_month
-- ----------------------------
INSERT INTO `bi_withdraw_order_month` VALUES (6, '2023-11', NULL, 300.00, 300.00, 3, 3, 0.00, '2023-11-21 21:49:01', '2023-11-21 21:49:01', '2023-11-21 21:49', NULL, NULL, NULL, NULL, NULL, NULL);

SET FOREIGN_KEY_CHECKS = 1;
