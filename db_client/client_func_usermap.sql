/**************************************************************************/
/*                                                                        */
/* Andrew G. West - client_func_usermap.sql - This is a stored function   */
/* that tracks "merged" accounts. If a person has two accounts (i,e, a    */
/* main one and a "public" one), this code allows those two accounts to   */
/* function as one in the content of the STiki tool.                      */
/*                                                                        */
/* At the current time, username mappings are defined internal to this    */
/* file. If the list becomes long, a table should be made.                */
/*                                                                        */
/**************************************************************************/

DELIMITER |

CREATE FUNCTION client_func_usermap (user VARCHAR(256))

RETURNS VARCHAR(256)

BEGIN

  IF user = 'Thine Antique Pen (public)' THEN
   RETURN('Thine Antique Pen');
  END IF;

  IF user = 'Vacation9 Public' THEN
   RETURN('Vacation9');
   
  ELSE
   RETURN(user);
  END IF;

END


/********************************** END ***********************************/