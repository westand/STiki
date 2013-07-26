-- --------------------------------------------------------------------------
--
-- Andrew G. West - db_schema.sql -- The following shows the STiki database
-- schema, as it stands on our production server. Piping this code into
-- another server should recreate the schema at that location.
--
-- This is achieved via usage of the [--no-data] flag to [mysqldump].
-- tables may be excluded using [--ignore-table=my_db_name.my_table_name]
-- (personal note: do this for "spam_corpus")
--
-- --------------------------------------------------------------------------
--
-- MySQL dump 10.13  Distrib 5.1.58, for redhat-linux-gnu (i386)
--
-- Host: localhost    Database: presta_stiki
-- 
-- ------------------------------------------------------
-- Server version 5.1.58

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `all_edits`
--

DROP TABLE IF EXISTS `all_edits`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `all_edits` (
  `R_ID` int(10) unsigned NOT NULL,
  `P_ID` int(10) unsigned NOT NULL,
  `TS` int(10) unsigned NOT NULL,
  `NS` int(10) unsigned NOT NULL,
  `TITLE` varchar(1024) NOT NULL,
  `USER` varchar(256) NOT NULL,
  `IS_IP` tinyint(1) NOT NULL,
  `COMMENT` varchar(2048) NOT NULL,
  `COUNTRY` char(2) NOT NULL,
  `RB` tinyint(1) NOT NULL,
  `OE` tinyint(1) NOT NULL,
  PRIMARY KEY (`R_ID`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `all_edits_old`
--

DROP TABLE IF EXISTS `all_edits_old`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `all_edits_old` (
  `R_ID` int(10) unsigned NOT NULL,
  `P_ID` int(10) unsigned NOT NULL,
  `TS` int(10) unsigned NOT NULL,
  `NS` int(10) unsigned NOT NULL,
  `TITLE` varchar(1024) NOT NULL,
  `USER` varchar(256) NOT NULL,
  `IS_IP` tinyint(1) NOT NULL,
  `COMMENT` varchar(2048) NOT NULL,
  `COUNTRY` char(2) NOT NULL,
  `RB` tinyint(1) NOT NULL,
  `OE` tinyint(1) NOT NULL,
  PRIMARY KEY (`R_ID`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `category_links`
--

DROP TABLE IF EXISTS `category_links`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `category_links` (
  `P_ID` int(8) unsigned NOT NULL DEFAULT '0',
  `CAT_ID` int(8) unsigned NOT NULL,
  KEY `cat_ind` (`CAT_ID`),
  KEY `pid_ind` (`P_ID`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `country`
--

DROP TABLE IF EXISTS `country`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `country` (
  `UNIX_DAY` int(10) unsigned NOT NULL,
  `COUNTRY` char(2) NOT NULL,
  `ALL_EDITS` int(10) unsigned NOT NULL,
  `BAD_EDITS` int(10) unsigned NOT NULL,
  KEY `look_ind` (`UNIX_DAY`,`COUNTRY`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `country_to_cont`
--

DROP TABLE IF EXISTS `country_to_cont`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `country_to_cont` (
  `CODE` char(2) NOT NULL,
  `NAME` varchar(255) NOT NULL,
  `FULL_NAME` varchar(255) NOT NULL,
  `ISO3` char(3) NOT NULL,
  `NUM_ID` smallint(3) unsigned zerofill NOT NULL,
  `CONTINENT` char(2) NOT NULL,
  PRIMARY KEY (`CODE`),
  KEY `name_ind` (`NAME`),
  KEY `full_name_ind` (`FULL_NAME`),
  KEY `iso3_ind` (`ISO3`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `default_queue`
--

DROP TABLE IF EXISTS `default_queue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `default_queue` (
  `DEF` tinyint(3) unsigned NOT NULL,
  `SYS_CODE` int(11) NOT NULL,
  `SYS_DESC` varchar(32) NOT NULL
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `features`
--

DROP TABLE IF EXISTS `features`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `features` (
  `LABEL` tinyint(1) NOT NULL,
  `R_ID` int(10) unsigned NOT NULL,
  `IS_IP` tinyint(1) NOT NULL,
  `REP_USER` double DEFAULT NULL,
  `REP_ARTICLE` double NOT NULL,
  `TOD` float NOT NULL,
  `DOW` smallint(6) NOT NULL,
  `TS_R` int(10) unsigned NOT NULL,
  `TS_LP` int(11) NOT NULL,
  `TS_RBU` int(11) NOT NULL,
  `COMM_LENGTH` int(10) unsigned NOT NULL,
  `BYTE_CHANGE` int(11) NOT NULL,
  `REP_COUNTRY` double NOT NULL,
  `NLP_DIRTY` int(10) unsigned NOT NULL,
  `NLP_CHAR_REP` int(10) unsigned NOT NULL,
  `NLP_UCASE` double unsigned NOT NULL,
  `NLP_ALPHA` double unsigned NOT NULL,
  PRIMARY KEY (`R_ID`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `features_old`
--

DROP TABLE IF EXISTS `features_old`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `features_old` (
  `LABEL` tinyint(1) NOT NULL,
  `R_ID` int(10) unsigned NOT NULL,
  `IS_IP` tinyint(1) NOT NULL,
  `REP_USER` double DEFAULT NULL,
  `REP_ARTICLE` double NOT NULL,
  `TOD` float NOT NULL,
  `DOW` smallint(6) NOT NULL,
  `TS_R` int(10) unsigned NOT NULL,
  `TS_LP` int(11) NOT NULL,
  `TS_RBU` int(11) NOT NULL,
  `COMM_LENGTH` int(10) unsigned NOT NULL,
  `BYTE_CHANGE` int(11) NOT NULL,
  `REP_COUNTRY` double NOT NULL,
  `NLP_DIRTY` int(10) unsigned NOT NULL,
  `NLP_CHAR_REP` int(10) unsigned NOT NULL,
  `NLP_UCASE` double unsigned NOT NULL,
  `NLP_ALPHA` double unsigned NOT NULL,
  PRIMARY KEY (`R_ID`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `features_spam`
--

DROP TABLE IF EXISTS `features_spam`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `features_spam` (
  `R_ID` int(10) unsigned NOT NULL,
  `LINK_ID` int(10) unsigned NOT NULL,
  `URL` varchar(1024) NOT NULL,
  `WIKI_AGGREGATES_ARTICLE_REP` double DEFAULT NULL,
  `WIKI_AGGREGATES_DOM_ADDITIONS_EVENTS_IN_6MOS` int(10) unsigned DEFAULT NULL,
  `WIKI_AGGREGATES_DOM_ADDITIONS_EVENTS_IN_DAY` int(10) unsigned DEFAULT NULL,
  `WIKI_AGGREGATES_DOM_ADDITIONS_EVENTS_IN_HOUR` int(10) unsigned DEFAULT NULL,
  `WIKI_AGGREGATES_DOM_ADDITIONS_EVENTS_IN_MONTH` int(10) unsigned DEFAULT NULL,
  `WIKI_AGGREGATES_DOM_ADDITIONS_EVENTS_IN_WEEK` int(10) unsigned DEFAULT NULL,
  `WIKI_AGGREGATES_DOM_DIVERSITY` double DEFAULT NULL,
  `WIKI_AGGREGATES_DOM_REP` double DEFAULT NULL,
  `WIKI_AGGREGATES_URL_ADDITIONS_EVENTS_IN_6MOS` int(10) unsigned DEFAULT NULL,
  `WIKI_AGGREGATES_URL_ADDITIONS_EVENTS_IN_DAY` int(10) unsigned DEFAULT NULL,
  `WIKI_AGGREGATES_URL_ADDITIONS_EVENTS_IN_HOUR` int(10) unsigned DEFAULT NULL,
  `WIKI_AGGREGATES_URL_ADDITIONS_EVENTS_IN_MONTH` int(10) unsigned DEFAULT NULL,
  `WIKI_AGGREGATES_URL_ADDITIONS_EVENTS_IN_WEEK` int(10) unsigned DEFAULT NULL,
  `WIKI_AGGREGATES_URL_DIVERSITY` double DEFAULT NULL,
  `WIKI_AGGREGATES_URL_REP` double DEFAULT NULL,
  `WIKI_INDEPENDENTS_ART_LEN` int(10) unsigned DEFAULT NULL,
  `WIKI_INDEPENDENTS_ART_REFERENCES` int(10) unsigned DEFAULT NULL,
  `WIKI_INDEPENDENTS_ART_TS_CREATION` int(10) unsigned DEFAULT NULL,
  `WIKI_INDEPENDENTS_META_COMM_LEN` int(10) unsigned DEFAULT NULL,
  `WIKI_INDEPENDENTS_META_COMM_LEN_WO_SECTION` int(10) unsigned DEFAULT NULL,
  `WIKI_INDEPENDENTS_META_DAY_WEEK` char(3) DEFAULT NULL,
  `WIKI_INDEPENDENTS_META_TIME_DAY` double DEFAULT NULL,
  `WIKI_LINK_DESC_LEN` int(10) unsigned DEFAULT NULL,
  `WIKI_LINK_IS_CITE` char(5) DEFAULT NULL,
  `WIKI_LINK_PLACEMENT` double DEFAULT NULL,
  `WIKI_URL_IS_DOMAIN` char(5) DEFAULT NULL,
  `WIKI_URL_LEN` int(11) DEFAULT NULL,
  `WIKI_URL_SUBDOMAINS` int(11) DEFAULT NULL,
  `WIKI_URL_TLD` char(4) DEFAULT NULL,
  `LANDING_SITE_COMPRESSABILITY` double DEFAULT NULL,
  `LANDING_SITE_DEGREE_COMMERCIAL` int(10) unsigned DEFAULT NULL,
  `LANDING_SITE_DEGREE_PROFANE` int(10) unsigned DEFAULT NULL,
  `LANDING_SITE_NUM_IMAGES` int(10) unsigned DEFAULT NULL,
  `LANDING_SITE_NUM_META_WORDS` int(10) unsigned DEFAULT NULL,
  `LANDING_SITE_PAGE_SIZE` int(10) unsigned DEFAULT NULL,
  `LANDING_SITE_RELEVANT` char(5) DEFAULT NULL,
  `LANDING_SITE_TITLE_LEN` int(10) unsigned DEFAULT NULL,
  `LANDING_SITE_VOCAB_LEN` double DEFAULT NULL,
  `ALEXA_ADULT_CONTENT` char(5) DEFAULT NULL,
  `ALEXA_BACKLINKS` int(10) unsigned DEFAULT NULL,
  `ALEXA_CONTINENT` char(2) DEFAULT NULL,
  `ALEXA_DELTAS` double DEFAULT NULL,
  `ALEXA_SITE_AGE` int(10) unsigned DEFAULT NULL,
  `ALEXA_SPEED_PERCENTILE` double DEFAULT NULL,
  `SAFE_BROWSE_GOOG_MALWARE` char(5) DEFAULT NULL,
  `SAFE_BROWSE_GOOG_PHISHING` char(5) DEFAULT NULL,
  KEY `rid_ind` (`R_ID`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `features_spam_traffic`
--

DROP TABLE IF EXISTS `features_spam_traffic`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `features_spam_traffic` (
  `R_ID` int(10) unsigned NOT NULL,
  `LINK_ID` int(10) unsigned NOT NULL,
  `1DAYS_PAGEVIEWS_PERMILL_DELTA` double DEFAULT NULL,
  `1DAYS_PAGEVIEWS_PERMILL_VALUE` double DEFAULT NULL,
  `1DAYS_PAGEVIEWS_PERUSER_DELTA` double DEFAULT NULL,
  `1DAYS_PAGEVIEWS_PERUSER_VALUE` double DEFAULT NULL,
  `1DAYS_PAGEVIEWS_RANK_DELTA` double DEFAULT NULL,
  `1DAYS_PAGEVIEWS_RANK_VALUE` double DEFAULT NULL,
  `1DAYS_RANK_DELTA` double DEFAULT NULL,
  `1DAYS_RANK_VALUE` double DEFAULT NULL,
  `1DAYS_REACH_PERMILL_DELTA` double DEFAULT NULL,
  `1DAYS_REACH_PERMILL_VALUE` double DEFAULT NULL,
  `1DAYS_REACH_RANK_DELTA` double DEFAULT NULL,
  `1DAYS_REACH_RANK_VALUE` double DEFAULT NULL,
  `7DAYS_PAGEVIEWS_PERMILL_DELTA` double DEFAULT NULL,
  `7DAYS_PAGEVIEWS_PERMILL_VALUE` double DEFAULT NULL,
  `7DAYS_PAGEVIEWS_PERUSER_DELTA` double DEFAULT NULL,
  `7DAYS_PAGEVIEWS_PERUSER_VALUE` double DEFAULT NULL,
  `7DAYS_PAGEVIEWS_RANK_DELTA` double DEFAULT NULL,
  `7DAYS_PAGEVIEWS_RANK_VALUE` double DEFAULT NULL,
  `7DAYS_RANK_DELTA` double DEFAULT NULL,
  `7DAYS_RANK_VALUE` double DEFAULT NULL,
  `7DAYS_REACH_PERMILL_DELTA` double DEFAULT NULL,
  `7DAYS_REACH_PERMILL_VALUE` double DEFAULT NULL,
  `7DAYS_REACH_RANK_DELTA` double DEFAULT NULL,
  `7DAYS_REACH_RANK_VALUE` double DEFAULT NULL,
  `1MOS_PAGEVIEWS_PERMILL_DELTA` double DEFAULT NULL,
  `1MOS_PAGEVIEWS_PERMILL_VALUE` double DEFAULT NULL,
  `1MOS_PAGEVIEWS_PERUSER_DELTA` double DEFAULT NULL,
  `1MOS_PAGEVIEWS_PERUSER_VALUE` double DEFAULT NULL,
  `1MOS_PAGEVIEWS_RANK_DELTA` double DEFAULT NULL,
  `1MOS_PAGEVIEWS_RANK_VALUE` double DEFAULT NULL,
  `1MOS_RANK_DELTA` double DEFAULT NULL,
  `1MOS_RANK_VALUE` double DEFAULT NULL,
  `1MOS_REACH_PERMILL_DELTA` double DEFAULT NULL,
  `1MOS_REACH_PERMILL_VALUE` double DEFAULT NULL,
  `1MOS_REACH_RANK_DELTA` double DEFAULT NULL,
  `1MOS_REACH_RANK_VALUE` double DEFAULT NULL,
  `3MOS_PAGEVIEWS_PERMILL_DELTA` double DEFAULT NULL,
  `3MOS_PAGEVIEWS_PERMILL_VALUE` double DEFAULT NULL,
  `3MOS_PAGEVIEWS_PERUSER_DELTA` double DEFAULT NULL,
  `3MOS_PAGEVIEWS_PERUSER_VALUE` double DEFAULT NULL,
  `3MOS_PAGEVIEWS_RANK_DELTA` double DEFAULT NULL,
  `3MOS_PAGEVIEWS_RANK_VALUE` double DEFAULT NULL,
  `3MOS_RANK_DELTA` double DEFAULT NULL,
  `3MOS_RANK_VALUE` double DEFAULT NULL,
  `3MOS_REACH_PERMILL_DELTA` double DEFAULT NULL,
  `3MOS_REACH_PERMILL_VALUE` double DEFAULT NULL,
  `3MOS_REACH_RANK_DELTA` double DEFAULT NULL,
  `3MOS_REACH_RANK_VALUE` double DEFAULT NULL,
  KEY `rid_ind` (`R_ID`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `feedback`
--

DROP TABLE IF EXISTS `feedback`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `feedback` (
  `R_ID` int(10) unsigned NOT NULL,
  `LABEL` int(11) NOT NULL,
  `TS_FB` int(10) unsigned NOT NULL,
  `USER_FB` varchar(256) NOT NULL,
  PRIMARY KEY (`R_ID`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `geo_city`
--

DROP TABLE IF EXISTS `geo_city`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `geo_city` (
  `ip_start` bigint(20) NOT NULL,
  `country_code` varchar(2) NOT NULL,
  `country_name` varchar(64) NOT NULL,
  `region_code` varchar(2) NOT NULL,
  `region_name` varchar(64) NOT NULL,
  `city` varchar(64) NOT NULL,
  `zipcode` varchar(6) NOT NULL,
  `latitude` float NOT NULL,
  `longitude` float NOT NULL,
  `timezone` varchar(4) NOT NULL,
  `gmtOffset` varchar(4) NOT NULL,
  `dstOffset` varchar(4) NOT NULL,
  UNIQUE KEY `ip_start` (`ip_start`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `geo_country`
--

DROP TABLE IF EXISTS `geo_country`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `geo_country` (
  `ip_start` bigint(20) NOT NULL,
  `ip_cidr` varchar(20) NOT NULL,
  `country_code` varchar(2) NOT NULL,
  `country_name` varchar(64) NOT NULL,
  UNIQUE KEY `ip_start` (`ip_start`),
  KEY `country` (`country_code`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hyperlinks`
--

DROP TABLE IF EXISTS `hyperlinks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `hyperlinks` (
  `R_ID` int(10) unsigned NOT NULL,
  `RBED` tinyint(1) unsigned NOT NULL,
  `URL` varchar(512) NOT NULL,
  `DESC` varchar(512) NOT NULL,
  `TS` int(10) unsigned NOT NULL,
  `USER` varchar(256) NOT NULL,
  KEY `rid_ind` (`R_ID`),
  KEY `url_rbed_ind` (`URL`,`RBED`),
  KEY `url_user_ind` (`URL`,`USER`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `log_client`
--

DROP TABLE IF EXISTS `log_client`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `log_client` (
  `USER` varchar(128) NOT NULL,
  `ACTION` varchar(32) NOT NULL,
  `TS` int(10) unsigned NOT NULL,
  KEY `ts_ind` (`TS`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oes_archive`
--

DROP TABLE IF EXISTS `oes_archive`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `oes_archive` (
  `R_ID` int(10) unsigned NOT NULL,
  `P_ID` int(10) unsigned NOT NULL,
  `TS` int(10) unsigned NOT NULL,
  `NS` int(10) unsigned NOT NULL,
  `USER` varchar(256) NOT NULL,
  `FLAG_RID` int(10) unsigned NOT NULL,
  `VIEWS` int(11) NOT NULL,
  PRIMARY KEY (`R_ID`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `offending_edits`
--

DROP TABLE IF EXISTS `offending_edits`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `offending_edits` (
  `R_ID` int(10) unsigned NOT NULL,
  `P_ID` int(10) unsigned NOT NULL,
  `TS` int(10) unsigned NOT NULL,
  `NS` int(10) unsigned NOT NULL,
  `USER` varchar(256) NOT NULL,
  `FLAG_RID` int(10) unsigned NOT NULL,
  `VIEWS` int(11) NOT NULL,
  PRIMARY KEY (`R_ID`),
  KEY `pid_ind` (`P_ID`) USING BTREE,
  KEY `user_ind` (`USER`) USING BTREE
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `queue_cbng`
--

DROP TABLE IF EXISTS `queue_cbng`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `queue_cbng` (
  `R_ID` int(10) unsigned NOT NULL,
  `P_ID` int(10) unsigned NOT NULL,
  `SCORE` double NOT NULL,
  `RES_EXP` int(10) unsigned NOT NULL DEFAULT '0',
  `RES_ID` int(10) unsigned NOT NULL DEFAULT '0',
  `PASS` varchar(4096) NOT NULL DEFAULT '',
  PRIMARY KEY (`P_ID`),
  KEY `res_id_ind` (`RES_ID`) USING BTREE,
  KEY `score_ind` (`SCORE`),
  KEY `rid_ind` (`R_ID`) USING BTREE
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `queue_spam`
--

DROP TABLE IF EXISTS `queue_spam`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `queue_spam` (
  `R_ID` int(10) unsigned NOT NULL,
  `P_ID` int(10) unsigned NOT NULL,
  `SCORE` double NOT NULL,
  `RES_EXP` int(10) unsigned NOT NULL DEFAULT '0',
  `RES_ID` int(10) unsigned NOT NULL DEFAULT '0',
  `PASS` varchar(4096) NOT NULL DEFAULT '',
  PRIMARY KEY (`P_ID`) USING BTREE,
  KEY `res_id_ind` (`RES_ID`) USING BTREE,
  KEY `score_ind` (`SCORE`),
  KEY `rid_ind` (`R_ID`) USING BTREE
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `queue_stiki`
--

DROP TABLE IF EXISTS `queue_stiki`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `queue_stiki` (
  `R_ID` int(10) unsigned NOT NULL,
  `P_ID` int(10) unsigned NOT NULL,
  `SCORE` double NOT NULL,
  `RES_EXP` int(10) unsigned NOT NULL DEFAULT '0',
  `RES_ID` int(10) unsigned NOT NULL DEFAULT '0',
  `PASS` varchar(4096) NOT NULL DEFAULT '',
  PRIMARY KEY (`P_ID`),
  KEY `res_id_ind` (`RES_ID`) USING BTREE,
  KEY `score_ind` (`SCORE`),
  KEY `rid_ind` (`R_ID`) USING BTREE
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `queue_wt`
--

DROP TABLE IF EXISTS `queue_wt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `queue_wt` (
  `R_ID` int(10) unsigned NOT NULL,
  `P_ID` int(10) unsigned NOT NULL,
  `SCORE` double NOT NULL,
  `RES_EXP` int(10) unsigned NOT NULL DEFAULT '0',
  `RES_ID` int(10) unsigned NOT NULL DEFAULT '0',
  `PASS` varchar(4096) NOT NULL DEFAULT '',
  PRIMARY KEY (`P_ID`),
  KEY `res_id_ind` (`RES_ID`) USING BTREE,
  KEY `score_ind` (`SCORE`),
  KEY `rid_ind` (`R_ID`) USING BTREE
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `scores_cbng`
--

DROP TABLE IF EXISTS `scores_cbng`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `scores_cbng` (
  `R_ID` int(10) unsigned NOT NULL,
  `SCORE` double NOT NULL,
  PRIMARY KEY (`R_ID`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `scores_spam`
--

DROP TABLE IF EXISTS `scores_spam`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `scores_spam` (
  `R_ID` int(10) unsigned NOT NULL,
  `SCORE` double NOT NULL,
  PRIMARY KEY (`R_ID`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `scores_stiki`
--

DROP TABLE IF EXISTS `scores_stiki`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `scores_stiki` (
  `R_ID` int(10) unsigned NOT NULL,
  `SCORE` double NOT NULL,
  PRIMARY KEY (`R_ID`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `scores_wt`
--

DROP TABLE IF EXISTS `scores_wt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `scores_wt` (
  `R_ID` int(10) unsigned NOT NULL,
  `SCORE` double NOT NULL,
  PRIMARY KEY (`R_ID`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stiki_status`
--

DROP TABLE IF EXISTS `stiki_status`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `stiki_status` (
  `NAME` varchar(128) NOT NULL,
  `VALUE` int(11) NOT NULL,
  PRIMARY KEY (`NAME`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stiki_status_spam`
--

DROP TABLE IF EXISTS `stiki_status_spam`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `stiki_status_spam` (
  `NAME` varchar(128) NOT NULL,
  `VALUE` int(11) NOT NULL,
  PRIMARY KEY (`NAME`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users_explicit`
--

DROP TABLE IF EXISTS `users_explicit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `users_explicit` (
  `USER` varchar(256) NOT NULL,
  PRIMARY KEY (`USER`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2012-06-01  3:06:14
