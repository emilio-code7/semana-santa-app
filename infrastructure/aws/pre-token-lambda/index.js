// Pre-token generation Lambda — injects hermandad_memberships claim from Cognito groups
// Trigger: V2.0 (USE with event.response.claimsAndScopeOverrideDetails)
// Input event: https://docs.aws.amazon.com/cognito/latest/developerguide/user-pool-lambda-pre-token-generation.html

exports.handler = async (event) => {
  const groups = event.request.groupConfiguration?.groupsToOverride || [];
  const memberships = groups
    .filter(g => g.startsWith('HERMANDAD_'))
    .map(g => {
      // Format: HERMANDAD_{hermandadId}_{role}
      const parts = g.split('_');
      if (parts.length < 3) return null;
      const role = parts.slice(2).join('_');
      return { hermandadId: parts[1], role };
    })
    .filter(Boolean);

  if (memberships.length > 0) {
    event.response = event.response || {};
    event.response.claimsAndScopeOverrideDetails = {
      accessTokenGeneration: {
        claimsToAddOrOverride: {
          hermandad_memberships: JSON.stringify(memberships),
        },
      },
    };
  }

  return event;
};
