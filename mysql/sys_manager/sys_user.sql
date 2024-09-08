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

 Date: 30/11/2023 14:00:43
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '用户名',
  `nickname` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '昵称',
  `gender` tinyint(1) NULL DEFAULT 1 COMMENT '性别：1-男 2-女',
  `password` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '密码',
  `avatar` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '' COMMENT '用户头像',
  `mobile` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '联系方式',
  `status` tinyint(1) NULL DEFAULT 1 COMMENT '用户状态：1-正常 0-禁用',
  `email` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '用户邮箱',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除标识：0-未删除；1-已删除',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `update_by` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '更新者',
  `create_by` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '创建者',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `login_name`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 40 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '用户信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (4, 'test', 'test', 1, '$2a$10$bkMZdHC9udanQzAkUguf2umSZaXhmKaaiooJhDbbYJjlIj9F07Pa2', '', '123456', 1, 'www.baidu.com', 0, NULL, NULL, NULL, NULL);
INSERT INTO `sys_user` VALUES (7, 'test2', 'test2', 1, '$2a$10$FUepUkVvm0D0.9Vfp5TcrOBd3qPrDd9VhXED2iOKQVFRr1DVj21n.', '', '123456', 1, 'www.baidu.com', 0, NULL, NULL, NULL, NULL);
INSERT INTO `sys_user` VALUES (10, 'xiaogui', '鬼哥', 1, '$2a$10$xFUZUzSnYIn1oZeafa9VU.XKiNWCQNkljBAsr0eBnIEmUXcQx.MQi', '', '123234938', 1, 'sdf@sdf.cad', 0, NULL, NULL, NULL, NULL);
INSERT INTO `sys_user` VALUES (11, 'xg', 'aaa', 1, '$2a$10$NcsMk9MSqI7SjoiA4MaXQOrwizgtRQJY7zu5/13tXhqthwzy92Pry', '', '123234938', 1, 'sdf@sdf.cad', 0, NULL, NULL, NULL, NULL);
INSERT INTO `sys_user` VALUES (14, '啊啊啊啊', '啊dasd', 1, '$2a$10$Th3x6D.hmoQ4EaxRbliOp.ER4hnhfhs.ldkYi03SK90OZ3GUbP6yi', '', '123234938', 1, 'sdf@sdf.cad', 0, NULL, NULL, NULL, NULL);
INSERT INTO `sys_user` VALUES (15, '啊啊啊啊2', '123', 1, '$2a$10$qXo48iUFOez5K/FB4ZM62uioxZzy2HSmOCS7FLEwrtC6rx/SZBrf6', '', '123234938', 1, 'sdf@sdf.cad', 0, NULL, NULL, NULL, NULL);
INSERT INTO `sys_user` VALUES (16, 'dev001', 'dev001', 1, '$2a$10$1LOAUyO/wYKgnhnUdnOZEufUseUw/kn93dhA3mFejGL9wVdDdABGi', '', '12345678', 1, 'www.baidu.com', 0, NULL, NULL, NULL, NULL);
INSERT INTO `sys_user` VALUES (17, 'dev002', 'dev002', 1, '$2a$10$cjC4hqbO4Tg8.woqJkEyYOlQKf4DZhOcjBqHFd9woxBbyQZBaq2Su', '', '12345678', 1, 'www.baidu.com', 0, NULL, NULL, NULL, NULL);
INSERT INTO `sys_user` VALUES (18, 'dev003', 'dev0032', 1, '$2a$10$71u690AVfGloQGHWyNbgp.8YDPS9m.5acf3qqJlKELqLp.FWaGisW', '', '12345678', 1, 'www.baidu.com', 0, NULL, NULL, NULL, NULL);
INSERT INTO `sys_user` VALUES (19, 'dev004', 'dev004', 1, '$2a$10$dpyFCgYzXpz/5y13rKbboOzmWS3gCAwopcO2PGPcU6qm31U4xBvaq', '', '12345678', 1, 'www.baidu.com', 0, NULL, NULL, NULL, NULL);
INSERT INTO `sys_user` VALUES (20, 'dev005', 'dev005', 1, '$2a$10$jn49wWmYvR5FrsxwurYjze3DCERQxW9QfzaBZRDg56//ntP4fhTYa', '', '12345678', 1, 'www.baidu.com', 0, NULL, NULL, NULL, NULL);
INSERT INTO `sys_user` VALUES (21, 'dev006', 'dev006', 1, '$2a$10$B8/z1TGCrjWM3qevLCo4Eeva1LEy20fC.iW3V2n0Pfk5ZQhYdWbby', '', '12345678', 1, 'www.baidu.com', 0, NULL, NULL, NULL, NULL);
INSERT INTO `sys_user` VALUES (22, 'dev007', 'dev007', 1, '$2a$10$ESPCWRG4S/IH3VirvL.KIOKlv1kRUSYFteucfy2L0lVoyqiMiv/WO', '', '12345678', 1, 'www.baidu.com', 0, NULL, NULL, NULL, NULL);
INSERT INTO `sys_user` VALUES (23, 'dev008', 'dev008', 1, '$2a$10$YkEUFRO.IfA7L/XQsmM6Nes4/y9RmhdMVcYJk2dCjdVZ1MBtJ7CQq', '', '12345678', 1, 'www.baidu.com', 0, NULL, NULL, NULL, NULL);
INSERT INTO `sys_user` VALUES (24, 'admin', 'admin', 1, '$2a$10$TXGbJVr0kh1eST5cfQae4.cpWrV.V3hQ1kJcn1Hjxsj8aBbN/jbE2', '', '12345678', 1, 'www.baidu.com', 0, NULL, NULL, NULL, NULL);
INSERT INTO `sys_user` VALUES (26, 'erde', 'erde', 1, '$2a$10$lREi5feip9sFL9J6nrSgf.NKFt3vLimdzJ3690DjHsUI7Ulqmnu2K', '', '12345678', 1, 'www.baidu.com', 0, NULL, NULL, NULL, NULL);
INSERT INTO `sys_user` VALUES (27, 'simon', 'simon', 1, '$2a$10$tlnKes7AUHrKKoPfhm0ePukjEFlnUGpGfJLwuSLivSJNUxd1V80zO', '', '12345678', 1, 'www.baidu.com', 0, NULL, NULL, NULL, NULL);
INSERT INTO `sys_user` VALUES (28, 'pony', 'pony', 1, '$2a$10$gInjWPHzeSGy6TSUJIYin.ONtqlw513R/p9VPA25PW9Cm7JWIkkMa', '', '12345678', 1, 'www.baidu.com', 0, NULL, NULL, NULL, NULL);
INSERT INTO `sys_user` VALUES (29, 'peiqi', 'peiqi', 1, '$2a$10$.GiEuOAm822K7WqlxQ.y/eNJ.zRgGIwDAX6QYMX4mba5EIxplXhDG', '', '12345678', 1, 'www.baidu.com', 0, NULL, NULL, NULL, NULL);
INSERT INTO `sys_user` VALUES (30, 'ace', 'ace', 1, '$2a$10$.kUrMT9F60HvdDTO1yFqe.wFdhawa8WliS148IMtVLhHqrpSEjjy.', '', '12345678', 1, 'www.baidu.com', 0, NULL, NULL, NULL, NULL);
INSERT INTO `sys_user` VALUES (31, 'louis', 'louis', 1, '$2a$10$43Lh6Nh5k/dZcJOyCa2oTe.HfMnE9zFZAUl0oQBSXgviTeQoU8YE.', '', '12345678', 1, 'www.baidu.com', 0, NULL, NULL, NULL, NULL);
INSERT INTO `sys_user` VALUES (32, 'test123', '123', 1, '$2a$10$gPO6B.BOGbPII/ZbEkuyOOD5jOtkpaSVvlPRUQENIqldKAoqs7xKy', '', '123', 1, 'www', 0, NULL, NULL, NULL, NULL);
INSERT INTO `sys_user` VALUES (33, 'test1234', '123', 1, '$2a$10$nP/WKJ5kpFT9y5fdf159gumdXHVt0a4nPCesnJt/2ESQKm5vJl2FG', '', '123', 1, 'www', 0, NULL, NULL, NULL, NULL);
INSERT INTO `sys_user` VALUES (35, 'test1235', '123', 1, '$2a$10$Txiy7Kdqa0NyGcGH1MSH.eXjt6pHP3rOKhX.9Rzpb31p/22XVZJ/O', '', '123', 1, 'www', 0, NULL, NULL, NULL, NULL);
INSERT INTO `sys_user` VALUES (38, 'test1236', '123', 1, '$2a$10$S432/CnmF/ADvkMzBEUc2eqrJN5xvDGhjnHoYK6l7r2VXXIuGABuS', '', '123', 1, 'www', 0, NULL, NULL, NULL, NULL);
INSERT INTO `sys_user` VALUES (39, 'test1237', '123', 1, '$2a$10$y4d/2lZhng7d/e3IzBZjdeRuovReeEJ8K0/yF6J6G60v/97r6WYWW', '', '123', 1, 'www', 0, '2023-11-11 17:17:53', '2023-11-11 17:17:53', 'louis', 'louis');

SET FOREIGN_KEY_CHECKS = 1;
