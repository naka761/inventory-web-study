package security;

public class PasswordHashGenerator {

    public static void main(String[] args) {

        if (args.length != 1) {
            System.err.println("Usage: PasswordHashGenerator <password>");
            return;
        }

        String hash =
                PasswordUtil.createHash(args[0]);

        System.out.println(hash);
    }
}
